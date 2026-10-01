package com.ai.repo.playground.rules;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.ai.repo.playground.rules.ShopRules.*;
import static org.junit.jupiter.api.Assertions.*;

class ShopRulesTest {
    private final ShopRules rules = new ShopRules();
    static Stream<Arguments> goldenCases() throws Exception {
        try (InputStream stream = ShopRulesTest.class.getResourceAsStream("/playground/shop-golden.json")) {
            JsonNode data = new ObjectMapper().readTree(stream);
            return java.util.stream.StreamSupport.stream(data.get("cases").spliterator(), false)
                    .map(node -> Arguments.of(node.get("id").asText(), node));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenCases")
    void agreesWithPublishedIndependentAccounts(String id, JsonNode fixture) {
        State state = play(id, Environment.valueOf(fixture.get("environment").asText()));
        Settlement result = rules.result(state);
        assertAll(
                () -> assertEquals(fixture.get("incomeMinor").asLong(), result.revenueMinor()),
                () -> assertEquals(fixture.get("expenseMinor").asLong(), result.expenseMinor()),
                () -> assertEquals(fixture.get("recoveryMinor").asLong(), result.recoveryMinor()),
                () -> assertEquals(fixture.get("returnedMinor").asLong(), result.returnedMinor()),
                () -> assertEquals(fixture.get("netMinor").asLong(), result.netMinor()),
                () -> assertEquals(fixture.get("soldUnits").asInt(), result.soldUnits()),
                () -> assertEquals(CAPITAL + result.netMinor(), result.returnedMinor()),
                () -> assertEquals(result.returnedMinor(), result.eachOwnerReturnedMinor() * 2));
        for (int i = 0; i < state.ledger().size(); i++) assertEquals(i + 1, state.ledger().get(i).sequence());
        assertEquals(state.cashMinor(), state.ledger().stream().mapToLong(Entry::amountMinor).sum());
    }

    private State play(String id, Environment environment) {
        if (id.startsWith("SHORT_")) {
            State shortState = standardDay(rules.open(rules.initialize(Mode.SHORT, Environment.NONE)), 4, false);
            if (id.equals("SHORT_ORDER")) {
                shortState = rules.acceptOrder(shortState, new Order("short-order", Sku.STANDARD, 6, 14, 2, 84, 0, false));
                shortState = rules.produce(rules.startDay(shortState, false), Sku.STANDARD, 6);
                shortState = rules.closeDay(rules.deliver(shortState, "short-order", 6));
            } else shortState = standardDay(shortState, 4, false);
            return rules.settle(shortState, false);
        }
        State state = rules.open(rules.initialize(environment));
        state = standardDay(state, 4, false);
        if (id.startsWith("ORDER_")) {
            state = rules.acceptOrder(state, new Order("customer-1", Sku.STANDARD, 6, 14, 2, 84, 0, false));
            state = rules.startDay(state, false);
            if (id.equals("ORDER_RESTORE")) state = rules.restoreCapacity(state);
            if (id.equals("ORDER_SHRINK")) state = rules.amendOrder(state, "customer-1", 2, 2);
            if (id.equals("ORDER_CANCEL")) {
                state = rules.cancelOrder(state, "customer-1");
                state = rules.produce(state, Sku.STANDARD, 2);
                state = rules.retail(state, Map.of(Sku.STANDARD, 16));
            } else if (id.equals("ORDER_EXTEND")) {
                state = rules.amendOrder(state, "customer-1", 6, 3);
                state = rules.produce(state, Sku.STANDARD, 2);
            } else {
                int count = id.equals("ORDER_SHRINK") ? 2 : 6;
                state = rules.produce(state, Sku.STANDARD, count);
                state = rules.deliver(state, "customer-1", count);
            }
            state = rules.closeDay(state);
            if (id.equals("ORDER_EXTEND")) {
                state = rules.startDay(state, false);
                state = rules.produce(state, Sku.STANDARD, 6);
                state = rules.deliver(state, "customer-1", 6);
                state = rules.retail(state, Map.of(Sku.STANDARD, 16));
                state = rules.closeDay(state);
            } else state = standardDay(state, 4, false);
        } else {
            int quantity = environment == Environment.MARKET_SLOWDOWN ? 2 : 4;
            state = standardDay(state, quantity, false);
            state = standardDay(state, id.equals("RENT_MOVE") ? 3 : quantity, id.equals("RENT_MOVE"));
        }
        return rules.settle(state, false);
    }
    private State standardDay(State state, int quantity, boolean move) {
        state = rules.startDay(state, move);
        state = rules.produce(state, Sku.STANDARD, quantity);
        state = rules.retail(state, Map.of(Sku.STANDARD, 16));
        return rules.closeDay(state);
    }

    @Test void shortModeEndsAfterTwoDaysWithoutPretendingThreeDaySuccess() {
        State state = standardDay(rules.open(rules.initialize(Mode.SHORT, Environment.NONE)), 4, false);
        state = rules.acceptOrder(state, new Order("short-order", Sku.STANDARD, 6, 14, 2, 84, 0, false));
        state = rules.produce(rules.startDay(state, false), Sku.STANDARD, 6);
        state = rules.deliver(state, "short-order", 6);
        State day2 = rules.closeDay(state);
        Settlement result = rules.result(rules.settle(day2, false));
        assertEquals("SHORT_COMPLETE", result.ending());
        assertEquals(800, result.netMinor());
        assertEquals(20_800, result.returnedMinor());
        assertThrows(IllegalArgumentException.class, () -> rules.startDay(day2, false));
        assertThrows(IllegalArgumentException.class, () -> rules.initialize(Mode.SHORT, Environment.POWER_OUTAGE));
    }
    @Test void shortModeCannotCommitAnOrderBeyondItsLastDay() {
        State state = standardDay(rules.open(rules.initialize(Mode.SHORT, Environment.NONE)), 4, false);
        assertThrows(IllegalArgumentException.class, () -> rules.acceptOrder(state,
                new Order("late", Sku.STANDARD, 6, 14, 3, 84, 0, false)));
    }

    @Test void capacityFailuresLeaveOriginalStateUnchanged() {
        State state = rules.startDay(rules.open(rules.initialize(Environment.POWER_OUTAGE)), false);
        state = rules.closeDay(state);
        State original = rules.startDay(state, false);
        assertThrows(IllegalArgumentException.class, () -> rules.produce(original, Sku.STANDARD, 3));
        assertEquals(2, original.capacityLeft());
        assertTrue(original.inventory().isEmpty());
    }
    @Test void saleAndRestorationCannotBeRepeated() {
        State state = rules.startDay(rules.open(rules.initialize(Environment.POWER_OUTAGE)), false);
        state = rules.closeDay(state);
        State restored = rules.restoreCapacity(rules.startDay(state, false));
        assertThrows(IllegalArgumentException.class, () -> rules.restoreCapacity(restored));
        State sold = rules.retail(rules.produce(restored, Sku.STANDARD, 2), Map.of(Sku.STANDARD, 16));
        assertThrows(IllegalArgumentException.class, () -> rules.retail(sold, Map.of(Sku.STANDARD, 16)));
    }
    @Test void oldStockKeepsOriginalBasisAndRecoveryRoundsEachItemDown() {
        State state = rules.startDay(rules.open(rules.initialize(Environment.MATERIAL_SURGE)), false);
        state = rules.produce(state, Sku.STANDARD, 2);
        state = rules.startDay(rules.closeDay(state), false);
        state = rules.produce(state, Sku.STANDARD, 2);
        state = rules.retail(state, Map.of(Sku.STANDARD, 20)); // two customers; consumes old batches first
        assertEquals(1, state.inventory().size());
        assertEquals(9, state.inventory().get(0).materialCoins());
        Settlement result = rules.result(rules.settle(rules.closeDay(state), true));
        assertEquals(800, result.recoveryMinor()); // two items at floor(9/2), not replacement price
    }
    @Test void twoSkusShareSixCustomersInsteadOfCreatingTwoPools() {
        State state = rules.startDay(rules.open(rules.initialize(Environment.NONE)), false);
        state = rules.produce(state, Sku.STANDARD, 2);
        state = rules.produce(state, Sku.SPECIAL, 2);
        state = rules.retail(state, Map.of(Sku.STANDARD, 12, Sku.SPECIAL, 22));
        assertEquals(4, state.soldUnits());
        assertEquals(6_800, state.ledger().stream().filter(e -> e.kind() == LedgerKind.SALE).mapToLong(Entry::amountMinor).sum());
    }
    @Test void contractsHaveNoSpendableIncomeBeforeDelivery() {
        State state = standardDay(rules.open(rules.initialize(Environment.NONE)), 4, false);
        State accepted = rules.acceptOrder(state, new Order("order", Sku.STANDARD, 6, 14, 2, 84, 0, false));
        assertEquals(state.cashMinor(), accepted.cashMinor());
        State cancelled = rules.cancelOrder(accepted, "order");
        assertEquals(state.cashMinor(), cancelled.cashMinor());
        assertThrows(IllegalArgumentException.class, () -> rules.acceptOrder(state,
                new Order("bad", Sku.STANDARD, 6, 14, 2, 83, 0, false)));
    }
    @Test void unresolvedContractsCannotBeSilentlyClosedOrSettled() {
        State state = standardDay(rules.open(rules.initialize(Environment.NONE)), 4, false);
        State accepted = rules.acceptOrder(state, new Order("order", Sku.STANDARD, 6, 14, 2, 84, 0, false));
        assertThrows(IllegalArgumentException.class, () -> rules.settle(accepted, true));
        State day2 = rules.startDay(accepted, false);
        assertThrows(IllegalArgumentException.class, () -> rules.closeDay(day2));
    }
    @Test void alreadyDeliveredStockCannotBeSoldAgain() {
        State state = standardDay(rules.open(rules.initialize(Environment.NONE)), 4, false);
        state = rules.acceptOrder(state, new Order("order", Sku.STANDARD, 6, 14, 2, 84, 0, false));
        state = rules.produce(rules.startDay(state, false), Sku.STANDARD, 6);
        State delivered = rules.deliver(state, "order", 6);
        State afterRetail = rules.retail(delivered, Map.of(Sku.STANDARD, 16));
        assertEquals(delivered.cashMinor(), afterRetail.cashMinor());
        assertEquals(0, rules.stock(afterRetail, Sku.STANDARD));
    }
    @Test void settlementIsFinalAndInventoryRecoveryCannotRepeat() {
        State state = rules.startDay(rules.open(rules.initialize(Environment.NONE)), false);
        state = rules.produce(state, Sku.STANDARD, 1);
        State settled = rules.settle(rules.closeDay(state), true);
        assertThrows(IllegalArgumentException.class, () -> rules.settle(settled, true));
        assertEquals(300, rules.result(settled).recoveryMinor());
        assertTrue(settled.inventory().isEmpty());
    }
    @Test void rentChangesOnlyAtRenewalAndMoveDoesNotDoubleChargeRent() {
        State state = standardDay(rules.open(rules.initialize(Environment.RENT_RENEWAL)), 4, false);
        State day2 = rules.startDay(state, false);
        assertEquals(1_200, -day2.ledger().stream().filter(e -> e.day() == 2 && e.reason().equals("RENT"))
                .mapToLong(Entry::amountMinor).sum());
        State day3 = rules.startDay(rules.closeDay(day2), true);
        assertEquals(600, -day3.ledger().stream().filter(e -> e.day() == 3 && e.reason().equals("RENT"))
                .mapToLong(Entry::amountMinor).sum());
        assertEquals(3, day3.capacityLeft());
    }
    @Test void packagingAppliesOnDeliveryDayEvenForOldStock() {
        State state = rules.startDay(rules.open(rules.initialize(Environment.PACKAGING_RULE)), false);
        state = rules.produce(state, Sku.STANDARD, 4);
        state = rules.startDay(rules.closeDay(state), false);
        state = rules.startDay(rules.closeDay(state), false);
        state = rules.retail(state, Map.of(Sku.STANDARD, 16));
        assertEquals(800, -state.ledger().stream().filter(e -> e.reason().equals("PACKAGING"))
                .mapToLong(Entry::amountMinor).sum());
    }
    @Test void invalidPricesAndPrematureSettlementAreRejected() {
        State state = rules.startDay(rules.open(rules.initialize(Environment.NONE)), false);
        assertThrows(IllegalArgumentException.class, () -> rules.retail(state, Map.of(Sku.STANDARD, 15)));
        State day1 = rules.closeDay(state);
        assertThrows(IllegalArgumentException.class, () -> rules.settle(day1, false));
        assertThrows(IllegalArgumentException.class, () -> rules.startDay(day1, true));
    }
    @Test void cashCannotBecomeNegativeOrBorrowFromFutureRevenue() {
        State state = rules.open(rules.initialize(Environment.POWER_OUTAGE));
        state = rules.produce(rules.startDay(state, false), Sku.SPECIAL, 3);
        state = rules.startDay(rules.closeDay(state), false);
        state = rules.produce(rules.restoreCapacity(state), Sku.SPECIAL, 3);
        State depleted = rules.closeDay(state);
        assertEquals(0, depleted.cashMinor());
        assertThrows(IllegalArgumentException.class, () -> rules.startDay(depleted, false));
    }
}
