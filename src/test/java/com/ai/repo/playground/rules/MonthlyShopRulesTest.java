package com.ai.repo.playground.rules;

import org.junit.jupiter.api.Test;
import static com.ai.repo.playground.rules.MonthlyShopRules.*;
import static org.junit.jupiter.api.Assertions.*;

class MonthlyShopRulesTest {
    final MonthlyShopRules rules = new MonthlyShopRules();
    State initial(Shock shock, int from, int through) { return rules.initialize(12, new Environment(shock, from, through)); }
    void reconciles(State state) {
        long profit = 0;
        long previousCash = CAPITAL;
        for (MonthlyReport report : state.reports()) {
            assertEquals(previousCash, report.openingCashMinor());
            assertEquals(report.openingCashMinor() + report.receiptsMinor() - report.paymentsMinor(), report.closingCashMinor());
            assertEquals(report.salesMinor() - report.costOfGoodsSoldMinor() - report.operatingExpenseMinor()
                    + report.liquidationReceiptsMinor() - report.liquidationBookCostMinor(), report.profitMinor());
            profit += report.profitMinor();
            assertEquals(profit, report.cumulativeProfitMinor());
            previousCash = report.closingCashMinor();
        }
        assertEquals(state.cashMinor(), previousCash);
        if (state.ending() != Ending.RUNNING) assertEquals(state.cashMinor() - CAPITAL, profit);
    }
    @Test void drawnEnvironmentIsBoundedAndItsLedgerEffectIsReproducible() {
        java.util.Random random = new java.util.Random(42);
        for (int horizon : new int[]{2, 12}) {
            for (int i = 0; i < 100; i++) {
                Environment draw = rules.drawEnvironment(horizon, random);
                assertNotEquals(Shock.NONE, draw.shock());
                assertTrue(draw.fromMonth() >= 2 && draw.throughMonth() <= horizon);
                State first = rules.initialize(horizon, draw);
                State replay = rules.initialize(horizon, draw);
                for (int month = 1; month <= draw.throughMonth(); month++) {
                    first = rules.advanceMonth(first, new Plan(4, 16, 0));
                    replay = rules.advanceMonth(replay, new Plan(4, 16, 0));
                }
                assertEquals(first, replay);
                assertTrue(first.reports().get(draw.fromMonth() - 1).events().contains(draw.shock().name()));
                reconciles(first);
            }
        }
    }
    @Test void approvedNpcOrderCompetesForTheSameInventoryAsRetail() {
        State first = rules.advanceMonth(initial(Shock.NONE, 1, 12), new Plan(4, 16, 0));
        State baseline = rules.advanceMonth(first, new Plan(4, 16, 0));
        NpcOrder offer = new NpcOrder("night-market-1", 2, 2, 18, 36);
        State accepted = rules.advanceMonth(first, new Plan(4, 16, 0), offer);
        MonthlyReport report = accepted.reports().get(1);
        assertTrue(report.events().contains("NPC_ORDER_FULFILLED"));
        assertEquals(4, report.soldUnits()); // Two NPC deliveries plus two retail sales, not six retail sales.
        assertEquals(6_800, report.salesMinor());
        assertEquals(2_400, report.costOfGoodsSoldMinor());
        assertEquals(400, accepted.cashMinor() - baseline.cashMinor());
        reconciles(accepted);
        assertThrows(IllegalArgumentException.class, () -> rules.advanceMonth(first,new Plan(4,16,0),
                new NpcOrder("wrong-month",3,2,18,36)));
    }
    @Test void acceptedNpcOrderCannotInventDeliveryWhenPowerOutageLimitsProduction() {
        State first = rules.advanceMonth(initial(Shock.POWER_OUTAGE, 2, 2), new Plan(4, 16, 0));
        State baseline = rules.advanceMonth(first, new Plan(4, 16, 0));
        State missed = rules.advanceMonth(first, new Plan(4, 16, 0),
                new NpcOrder("night-market-1",2,3,18,54));
        MonthlyReport report = missed.reports().get(1);
        assertTrue(report.events().contains("NPC_ORDER_UNFILLED"));
        assertTrue(report.events().contains("PRODUCTION_REDUCED"));
        assertEquals(baseline.cashMinor(), missed.cashMinor());
        assertEquals(2, report.soldUnits());
        reconciles(missed);
        assertThrows(IllegalArgumentException.class, () -> new NpcOrder("overspent",2,3,18,53));
    }
    @Test void twelveVirtualMonthsEndWithoutThirteenthOrWaiting() {
        State state = initial(Shock.NONE, 1, 12);
        for (int i = 0; i < 12; i++) state = rules.advanceMonth(state, new Plan(4, 16, 0));
        assertEquals(Ending.YEAR_COMPLETE, state.ending());
        assertEquals(12, state.reports().size());
        assertEquals(40_000, state.cashMinor()); // 200 - 40 + 12*(64 - 24 - 20)
        assertEquals(20_000, rules.summary(state).netProfitMinor());
        reconciles(state);
        State ended = state;
        assertThrows(IllegalArgumentException.class, () -> rules.advanceMonth(ended, new Plan(4, 16, 0)));
        assertThrows(IllegalArgumentException.class, () -> rules.close(ended));
    }
    @Test void inventoryIsNotExpensedUntilSoldAndKeepsOldMaterialCost() {
        State one = rules.advanceMonth(initial(Shock.MATERIAL_SURGE, 2, 2), new Plan(6, 20, 0));
        assertEquals(2, one.reports().get(0).soldUnits());
        assertEquals(2_400, one.reports().get(0).closingInventoryMinor());
        assertEquals(1_200, one.reports().get(0).costOfGoodsSoldMinor());
        State two = rules.advanceMonth(one, new Plan(2, 16, 0));
        assertEquals(2_400, two.reports().get(1).costOfGoodsSoldMinor());
        assertEquals(1_800, two.reports().get(1).closingInventoryMinor());
        reconciles(two);
        reconciles(rules.close(two));
    }
    @Test void negativeMonthDoesNotEndGame() {
        State state = rules.advanceMonth(initial(Shock.NONE, 1, 12), new Plan(0, 16, 0));
        assertEquals(-6_000, state.reports().get(0).profitMinor());
        assertEquals(Ending.RUNNING, state.ending());
    }
    @Test void bankruptcyReportsActualMonthsAndFailedNextOpeningSeparately() {
        State state = initial(Shock.NONE, 1, 12);
        while (state.ending() == Ending.RUNNING) state = rules.advanceMonth(state, new Plan(0, 16, 0));
        assertEquals(Ending.BUSINESS_FAILURE, state.ending());
        assertEquals(8, state.operatedMonths());
        assertEquals(9, state.failedOpeningMonth());
        assertEquals(8, state.reports().size());
        assertEquals(0, state.cashMinor());
        reconciles(state);
    }
    @Test void closureLiquidatesOnceAndNeverChargesFutureMonths() {
        State state = rules.close(rules.advanceMonth(initial(Shock.NONE, 1, 12), new Plan(6, 20, 0)));
        assertEquals(Ending.ACTIVE_CLOSURE, state.ending());
        assertEquals(1, state.operatedMonths());
        assertEquals(15_600, state.cashMinor());
        assertEquals(1_200, state.reports().get(0).liquidationReceiptsMinor());
        assertEquals(2_400, state.reports().get(0).liquidationBookCostMinor());
        reconciles(state);
    }
    @Test void expiredEnvironmentStopsAffectingLaterMonths() {
        State one = rules.advanceMonth(initial(Shock.RENT_RENEWAL, 2, 2), new Plan(4, 16, 0));
        State two = rules.advanceMonth(one, new Plan(4, 16, 0));
        State three = rules.advanceMonth(two, new Plan(4, 16, 0));
        assertEquals(2_800, two.reports().get(1).operatingExpenseMinor());
        assertEquals(2_000, three.reports().get(2).operatingExpenseMinor());
        reconciles(three);
    }
    @Test void reserveAndPowerOutageReduceProductionWithoutNegativeCash() {
        State state = rules.advanceMonth(initial(Shock.POWER_OUTAGE, 1, 1), new Plan(6, 16, 14_000));
        assertTrue(state.reports().get(0).events().contains("PRODUCTION_REDUCED"));
        assertEquals(0, state.reports().get(0).soldUnits());
        assertEquals(14_000, state.cashMinor());
        reconciles(state);
    }
    @Test void shortModeUsesTwoMonthsAndLiquidatesOnFinalReport() {
        State state = rules.initialize(2, new Environment(Shock.NONE, 1, 12));
        for (int i = 0; i < 2; i++) state = rules.advanceMonth(state, new Plan(6, 20, 0));
        assertEquals(Ending.SHORT_COMPLETE, state.ending());
        assertEquals(2, rules.summary(state).operatedMonths());
        assertEquals(0, state.inventory().size());
        reconciles(state);
    }
    @Test void allEnvironmentRunsReconcileEveryMonthAndAtEnd() {
        for (Shock shock : Shock.values()) {
            State state = initial(shock, 2, 6);
            while (state.ending() == Ending.RUNNING) {
                state = rules.advanceMonth(state, new Plan(6, 16, 0));
                reconciles(state);
            }
            Summary summary = rules.summary(state);
            assertEquals(summary.returnedCapitalMinor(), summary.ownerOneReturnedMinor() + summary.ownerTwoReturnedMinor());
        }
    }
    @Test void validatesHorizonEnvironmentAndPlan() {
        assertThrows(IllegalArgumentException.class, () -> rules.initialize(13, new Environment(Shock.NONE, 1, 12)));
        assertThrows(IllegalArgumentException.class, () -> new Environment(Shock.MARKET_SLOWDOWN, 5, 4));
        assertThrows(IllegalArgumentException.class, () -> new Plan(7, 16, 0));
        assertThrows(IllegalArgumentException.class, () -> new Plan(1, 15, 0));
    }
    @Test void inventoryLiquidationCanRescueNextOpeningBeforeFailure() {
        MonthlyReport report = new MonthlyReport(1, CAPITAL, 1_000, 0, 19_000, 0, 0,
                13_000, 0, 6_000, 0, 0, -13_000, -13_000, 0, java.util.List.of());
        State state = new State(12, 1, null, 1_000, java.util.List.of(new Batch(10, 600)),
                java.util.List.of(report), new Environment(Shock.NONE, 1, 12), Ending.RUNNING);
        state = rules.advanceMonth(state, new Plan(0, 16, 0));
        assertEquals(Ending.RUNNING, state.ending());
        assertEquals(2, state.operatedMonths());
        assertNull(state.failedOpeningMonth());
        assertEquals(2_000, state.cashMinor());
        assertEquals(3_000, state.reports().get(0).liquidationReceiptsMinor());
        reconciles(state);
    }
    @Test void closureBeforeOpeningReturnsAllCapitalWithoutImaginaryReport() {
        Summary summary = rules.summary(rules.close(initial(Shock.NONE, 1, 12)));
        assertEquals(0, summary.operatedMonths());
        assertTrue(summary.reports().isEmpty());
        assertEquals(CAPITAL, summary.returnedCapitalMinor());
        assertEquals(0, summary.netProfitMinor());
    }
}
