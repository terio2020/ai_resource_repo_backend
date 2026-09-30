package com.ai.repo.playground.rules;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic v0.3 accounting kernel. No model, network or persistence side effects.
 * Callers must validate task ownership, proposal signatures and idempotency before reduction.
 * All money is integer minor units: 100 minor units = one virtual coin.
 */
public final class ShopRules {
    public static final String RULE_VERSION = "0.3";
    public static final long CAPITAL = 20_000;
    public enum Mode { SHORT, FULL }
    public enum Phase { READY, BETWEEN_DAYS, TRADING, SETTLED }
    public enum Environment { NONE, MATERIAL_SURGE, RENT_RENEWAL, MARKET_SLOWDOWN, PACKAGING_RULE, POWER_OUTAGE }
    public enum Sku {
        STANDARD(6, 1, List.of(12, 16, 20)), SPECIAL(10, 2, List.of(22, 28, 34));
        final int materialCoins;
        final int capacity;
        final List<Integer> prices;
        Sku(int materialCoins, int capacity, List<Integer> prices) {
            this.materialCoins = materialCoins;
            this.capacity = capacity;
            this.prices = prices;
        }
    }
    public enum LedgerKind { CAPITAL, EXPENSE, SALE, RECOVERY }
    public record Entry(long sequence, int day, LedgerKind kind, String reason, long amountMinor) {}
    public record Batch(Sku sku, int quantity, int materialCoins) {}
    /** Terms are already accepted by both partners and the NPC, outside this kernel. */
    public record Order(String id, Sku sku, int quantity, int priceCoins, int deadlineDay,
                        int npcBudgetCoins, int delivered, boolean cancelled) {
        public Order {
            require(id != null && !id.isBlank() && id.length() <= 100, "INVALID_ORDER_ID");
            require(sku != null && quantity > 0 && quantity <= 18, "INVALID_ORDER_QUANTITY");
            require(priceCoins > 0 && priceCoins <= 34 && deadlineDay >= 1 && deadlineDay <= 3,
                    "INVALID_ORDER_TERMS");
            require(npcBudgetCoins >= quantity * priceCoins, "NPC_BUDGET_EXCEEDED");
            require(delivered >= 0 && delivered <= quantity, "INVALID_DELIVERY_COUNT");
        }
        public int remaining() { return quantity - delivered; }
    }
    public record State(Mode mode, Phase phase, Environment environment, int day, long cashMinor,
                        int capacityLeft, boolean retailDone, boolean restored,
                        List<Batch> inventory, Map<String, Order> orders, List<Entry> ledger,
                        int soldUnits) {
        public State {
            inventory = List.copyOf(inventory);
            orders = Map.copyOf(orders);
            ledger = List.copyOf(ledger);
        }
    }
    public record Settlement(String ending, long revenueMinor, long recoveryMinor,
                             long expenseMinor, long netMinor, long returnedMinor,
                             long eachOwnerReturnedMinor, int soldUnits) {}

    public State initialize(Environment environment) {
        return initialize(Mode.FULL, environment);
    }

    public State initialize(Mode mode, Environment environment) {
        require(mode != null && environment != null, "INVALID_MODE_OR_ENVIRONMENT");
        require(mode != Mode.SHORT || environment == Environment.NONE, "SHORT_HAS_NO_ENVIRONMENT_SHOCK");
        return new State(mode, Phase.READY, environment, 0, CAPITAL, 0, false, false,
                List.of(), Map.of(), List.of(new Entry(1, 0, LedgerKind.CAPITAL, "OWNER_CAPITAL", CAPITAL)), 0);
    }

    public State open(State state) {
        require(state.phase() == Phase.READY, "ALREADY_OPENED");
        State next = payment(state, LedgerKind.EXPENSE, "SETUP", -4_000);
        return copy(next, Phase.BETWEEN_DAYS, 0, 0, false, false, next.inventory(), next.orders(), 0);
    }

    /** Moving is an approved day-three choice; the unchanged demand pool is intentional. */
    public State startDay(State state, boolean move) {
        require(state.phase() == Phase.BETWEEN_DAYS && state.day() < lastDay(state), "INVALID_DAY_TRANSITION");
        int day = state.day() + 1;
        require(!move || (state.environment() == Environment.RENT_RENEWAL && day == 3), "MOVE_NOT_AVAILABLE");
        int rent = move ? 6 : state.environment() == Environment.RENT_RENEWAL && day == 3 ? 20 : 12;
        int capacity = move ? 3 : state.environment() == Environment.POWER_OUTAGE && day == 2 ? 2 : 6;
        long required = (rent + 8L + (move ? 4 : 0)) * 100;
        require(state.cashMinor() >= required, "INSUFFICIENT_CASH");
        State next = copy(state, Phase.TRADING, day, capacity, false, false,
                state.inventory(), state.orders(), state.soldUnits());
        if (move) next = payment(next, LedgerKind.EXPENSE, "MOVE", -400);
        next = payment(next, LedgerKind.EXPENSE, "RENT", -rent * 100L);
        return payment(next, LedgerKind.EXPENSE, "OPERATING", -800);
    }

    public State restoreCapacity(State state) {
        trading(state);
        require(state.environment() == Environment.POWER_OUTAGE && state.day() == 2 && !state.restored(),
                "RESTORE_NOT_AVAILABLE");
        State next = payment(state, LedgerKind.EXPENSE, "RESTORE_CAPACITY", -6_000);
        return copy(next, next.phase(), next.day(), next.capacityLeft() + 4, next.retailDone(), true,
                next.inventory(), next.orders(), next.soldUnits());
    }

    public State produce(State state, Sku sku, int quantity) {
        trading(state);
        require(sku != null && quantity > 0 && quantity <= 6, "INVALID_PRODUCTION");
        require(quantity * sku.capacity <= state.capacityLeft(), "CAPACITY_EXCEEDED");
        int cost = sku == Sku.STANDARD && state.environment() == Environment.MATERIAL_SURGE
                && state.day() >= 2 ? 9 : sku.materialCoins;
        State next = payment(state, LedgerKind.EXPENSE, "MATERIAL_" + sku.name(), -quantity * cost * 100L);
        List<Batch> batches = new ArrayList<>(next.inventory());
        batches.add(new Batch(sku, quantity, cost));
        return copy(next, next.phase(), next.day(), next.capacityLeft() - quantity * sku.capacity,
                next.retailDone(), next.restored(), batches, next.orders(), next.soldUnits());
    }

    /** One bounded customer pool per day, shared by all SKUs. Prices are approved price tiers. */
    public State retail(State state, Map<Sku, Integer> prices) {
        trading(state);
        require(!state.retailDone(), "CUSTOMER_POOL_ALREADY_USED");
        require(prices != null && !prices.isEmpty(), "INVALID_PRICE_PLAN");
        Map<Sku, Integer> validated = new EnumMap<>(Sku.class);
        prices.forEach((sku, price) -> {
            require(sku != null && price != null && sku.prices.contains(price), "INVALID_PRICE_TIER");
            validated.put(sku, price);
        });
        int[] willingness = state.environment() == Environment.MARKET_SLOWDOWN && state.day() >= 2
                ? new int[]{10, 10, 12, 12, 20, 26} : new int[]{12, 12, 16, 16, 28, 34};
        State next = state;
        for (int maximum : willingness) {
            Sku selected = null;
            int lowest = Integer.MAX_VALUE;
            for (Sku sku : Sku.values()) {
                Integer price = validated.get(sku);
                if (price != null && price <= maximum && price < lowest && stock(next, sku) > 0) {
                    selected = sku;
                    lowest = price;
                }
            }
            if (selected != null && next.cashMinor() >= packaging(next, 1)) {
                if (packaging(next, 1) > 0) next = payment(next, LedgerKind.EXPENSE, "PACKAGING", -packaging(next, 1));
                next = remove(next, selected, 1);
                next = payment(next, LedgerKind.SALE, "RETAIL_" + selected.name(), lowest * 100L);
                next = copy(next, next.phase(), next.day(), next.capacityLeft(), next.retailDone(),
                        next.restored(), next.inventory(), next.orders(), next.soldUnits() + 1);
            }
        }
        return copy(next, next.phase(), next.day(), next.capacityLeft(), true, next.restored(),
                next.inventory(), next.orders(), next.soldUnits());
    }

    public State acceptOrder(State state, Order order) {
        require(state.phase() == Phase.BETWEEN_DAYS, "ORDER_WINDOW_CLOSED");
        require(order != null && !state.orders().containsKey(order.id()), "DUPLICATE_ORDER");
        require(order.delivered() == 0 && !order.cancelled() && order.deadlineDay() > state.day() && order.deadlineDay() <= lastDay(state), "INVALID_NEW_ORDER");
        Map<String, Order> orders = new LinkedHashMap<>(state.orders());
        orders.put(order.id(), order);
        return copy(state, state.phase(), state.day(), state.capacityLeft(), state.retailDone(), state.restored(),
                state.inventory(), orders, state.soldUnits());
    }

    public State deliver(State state, String id, int quantity) {
        trading(state);
        Order order = state.orders().get(id);
        require(order != null && !order.cancelled() && state.day() <= order.deadlineDay(), "ORDER_NOT_DELIVERABLE");
        require(quantity > 0 && quantity <= order.remaining() && stock(state, order.sku()) >= quantity, "INVALID_DELIVERY");
        require(state.cashMinor() >= packaging(state, quantity), "INSUFFICIENT_CASH");
        State next = state;
        if (packaging(next, quantity) > 0) next = payment(next, LedgerKind.EXPENSE, "PACKAGING", -packaging(next, quantity));
        next = remove(next, order.sku(), quantity);
        next = payment(next, LedgerKind.SALE, "ORDER_" + id, quantity * order.priceCoins() * 100L);
        Map<String, Order> orders = new LinkedHashMap<>(next.orders());
        orders.put(id, new Order(id, order.sku(), order.quantity(), order.priceCoins(), order.deadlineDay(),
                order.npcBudgetCoins(), order.delivered() + quantity, false));
        return copy(next, next.phase(), next.day(), next.capacityLeft(), next.retailDone(), next.restored(),
                next.inventory(), orders, next.soldUnits() + quantity);
    }

    /** Only invoked after both partners and the original customer accept these new terms. */
    public State amendOrder(State state, String id, int quantity, int deadlineDay) {
        require(state.phase() == Phase.TRADING || state.phase() == Phase.BETWEEN_DAYS, "ORDER_WINDOW_CLOSED");
        Order order = state.orders().get(id);
        require(order != null && !order.cancelled() && order.remaining() > 0, "ORDER_NOT_AMENDABLE");
        require(quantity >= order.delivered() && deadlineDay >= state.day() && deadlineDay <= lastDay(state), "INVALID_AMENDMENT");
        Order amended = new Order(id, order.sku(), quantity, order.priceCoins(), deadlineDay,
                order.npcBudgetCoins(), order.delivered(), false);
        Map<String, Order> orders = new LinkedHashMap<>(state.orders());
        orders.put(id, amended);
        return copy(state, state.phase(), state.day(), state.capacityLeft(), state.retailDone(), state.restored(),
                state.inventory(), orders, state.soldUnits());
    }

    /** First version's pre-agreed cancellation has no fine and no unpaid income. */
    public State cancelOrder(State state, String id) {
        require(state.phase() != Phase.SETTLED, "ALREADY_SETTLED");
        Order order = state.orders().get(id);
        require(order != null && !order.cancelled() && order.remaining() > 0, "ORDER_NOT_CANCELLABLE");
        Map<String, Order> orders = new LinkedHashMap<>(state.orders());
        orders.put(id, new Order(id, order.sku(), order.quantity(), order.priceCoins(), order.deadlineDay(),
                order.npcBudgetCoins(), order.delivered(), true));
        return copy(state, state.phase(), state.day(), state.capacityLeft(), state.retailDone(), state.restored(),
                state.inventory(), orders, state.soldUnits());
    }

    public State closeDay(State state) {
        trading(state);
        // Contract cancellation is a separate explicit operation, never invented here.
        require(state.orders().values().stream().noneMatch(o -> !o.cancelled() && o.remaining() > 0
                && o.deadlineDay() <= state.day()), "UNRESOLVED_DUE_ORDER");
        return copy(state, Phase.BETWEEN_DAYS, state.day(), 0, state.retailDone(), state.restored(),
                state.inventory(), state.orders(), state.soldUnits());
    }

    public State settle(State state, boolean earlyClosure) {
        require(state.phase() == Phase.BETWEEN_DAYS && (state.day() == lastDay(state) || earlyClosure), "INVALID_SETTLEMENT_PHASE");
        require(state.orders().values().stream().noneMatch(o -> !o.cancelled() && o.remaining() > 0), "UNRESOLVED_ORDER");
        long recovery = state.inventory().stream()
                .mapToLong(b -> b.quantity() * (b.materialCoins() / 2L) * 100).sum();
        State next = recovery == 0 ? state : payment(state, LedgerKind.RECOVERY, "STOCK_RECOVERY", recovery);
        return copy(next, Phase.SETTLED, next.day(), 0, next.retailDone(), next.restored(), List.of(), next.orders(), next.soldUnits());
    }

    public Settlement result(State state) {
        require(state.phase() == Phase.SETTLED, "NOT_SETTLED");
        long revenue = total(state, LedgerKind.SALE);
        long recovery = total(state, LedgerKind.RECOVERY);
        long expense = -total(state, LedgerKind.EXPENSE);
        long net = revenue + recovery - expense;
        require(state.cashMinor() == CAPITAL + net, "LEDGER_INVARIANT_FAILED");
        boolean fulfilled = state.orders().values().stream().allMatch(o -> !o.cancelled() && o.remaining() == 0);
        String ending = state.day() < lastDay(state) ? "ACTIVE_CLOSURE"
                : state.mode() == Mode.SHORT ? "SHORT_COMPLETE" : net >= 2_000 && state.soldUnits() > 0 && fulfilled
                ? "TARGET_SUCCESS" : net >= 0 ? "PROFIT_OR_BREAK_EVEN" : "LOSS_CLOSURE";
        return new Settlement(ending, revenue, recovery, expense, net, state.cashMinor(), state.cashMinor() / 2, state.soldUnits());
    }

    public int stock(State state, Sku sku) {
        return state.inventory().stream().filter(b -> b.sku() == sku).mapToInt(Batch::quantity).sum();
    }
    private long packaging(State state, int quantity) {
        return state.environment() == Environment.PACKAGING_RULE && state.day() == 3 ? quantity * 200L : 0;
    }
    private State remove(State state, Sku sku, int quantity) {
        int left = quantity;
        List<Batch> batches = new ArrayList<>();
        for (Batch batch : state.inventory()) {
            int consumed = batch.sku() == sku ? Math.min(left, batch.quantity()) : 0;
            left -= consumed;
            if (batch.quantity() > consumed) batches.add(new Batch(batch.sku(), batch.quantity() - consumed, batch.materialCoins()));
        }
        require(left == 0, "INSUFFICIENT_STOCK");
        return copy(state, state.phase(), state.day(), state.capacityLeft(), state.retailDone(), state.restored(),
                batches, state.orders(), state.soldUnits());
    }
    private State payment(State state, LedgerKind kind, String reason, long amount) {
        long cash = Math.addExact(state.cashMinor(), amount);
        require(cash >= 0, "INSUFFICIENT_CASH");
        List<Entry> entries = new ArrayList<>(state.ledger());
        entries.add(new Entry(entries.size() + 1L, state.day(), kind, reason, amount));
        return new State(state.mode(), state.phase(), state.environment(), state.day(), cash, state.capacityLeft(),
                state.retailDone(), state.restored(), state.inventory(), state.orders(), entries, state.soldUnits());
    }
    private long total(State state, LedgerKind kind) {
        return state.ledger().stream().filter(e -> e.kind() == kind).mapToLong(Entry::amountMinor).sum();
    }
    private State copy(State s, Phase phase, int day, int capacity, boolean retail, boolean restored,
                       List<Batch> stock, Map<String, Order> orders, int sold) {
        return new State(s.mode(), phase, s.environment(), day, s.cashMinor(), capacity, retail, restored, stock, orders, s.ledger(), sold);
    }
    private static int lastDay(State state) { return state.mode() == Mode.SHORT ? 2 : 3; }
    private static void trading(State state) { require(state.phase() == Phase.TRADING, "NOT_TRADING"); }
    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalArgumentException(code);
    }
}
