package com.ai.repo.playground.rules;

import java.util.ArrayList;
import java.util.List;

/** Pure, bounded v5 strategy economics. A room service must persist the signed strategy,
 * signal and both Agents' decisions before invoking this reducer.
 */
public final class V5ShopRules {
    public enum Audience { NIGHT_READERS, COMMUTERS, STUDENTS }
    public enum Channel { NONE, FLYERS, LOCAL_EVENT }
    public enum ServicePromise { QUIET, FAST, COMMUNITY }
    public enum Signal { NORMAL, MARKET_SHIFT, MATERIAL_SURGE, RENT_RISE, PACKAGING_CHANGE, POWER_OUTAGE, COMPETITOR }
    public enum Response { KEEP_IDENTITY, PROMOTE, TEMPORARY_PIVOT }

    public record Strategy(int produceUnits, int unitPriceCoins, long minimumReserveMinor,
                           Audience audience, Channel channel, ServicePromise servicePromise,
                           long monthlyMarketingBudgetMinor) {
        public Strategy {
            new MonthlyShopRules.Plan(produceUnits, unitPriceCoins, minimumReserveMinor);
            require(audience != null && channel != null && servicePromise != null,
                    "INVALID_V5_STRATEGY");
            require(monthlyMarketingBudgetMinor >= 0 && monthlyMarketingBudgetMinor <= 1_200
                    && monthlyMarketingBudgetMinor % 300 == 0, "INVALID_V5_STRATEGY");
            require((channel == Channel.NONE) == (monthlyMarketingBudgetMinor == 0),
                    "INVALID_V5_STRATEGY");
            if (channel == Channel.LOCAL_EVENT)
                require(monthlyMarketingBudgetMinor >= 600, "INVALID_V5_STRATEGY");
        }
    }

    public record MonthResult(MonthlyShopRules.State game, Response response,
                              long marketingSpentMinor, int addedBuyers,
                              boolean budgetGuardTriggered) {}

    private final MonthlyShopRules ledger = new MonthlyShopRules();

    /** Only the server chooses the signal and this method's finite response values.
     * All cost and demand changes are deterministic; model prose cannot modify them.
     */
    public MonthResult advance(MonthlyShopRules.State game, Strategy strategy,
                               Signal signal, Response response) {
        require(game != null && strategy != null && signal != null && response != null,
                "INVALID_V5_MONTH");
        int month = game.operatedMonths() + 1;
        require(month <= game.horizonMonths(), "GAME_FINISHED");
        MonthlyShopRules.Shock expected = switch (signal) {
            case MARKET_SHIFT -> MonthlyShopRules.Shock.MARKET_SLOWDOWN;
            case MATERIAL_SURGE -> MonthlyShopRules.Shock.MATERIAL_SURGE;
            case RENT_RISE -> MonthlyShopRules.Shock.RENT_RENEWAL;
            case PACKAGING_CHANGE -> MonthlyShopRules.Shock.PACKAGING_RULE;
            case POWER_OUTAGE -> MonthlyShopRules.Shock.POWER_OUTAGE;
            case NORMAL, COMPETITOR -> MonthlyShopRules.Shock.NONE;
        };
        MonthlyShopRules.Environment environment = game.environment();
        MonthlyShopRules.Shock active = month >= environment.fromMonth() && month <= environment.throughMonth()
                ? environment.shock() : MonthlyShopRules.Shock.NONE;
        require(active == expected, "SIGNAL_ENVIRONMENT_MISMATCH");
        require(signal != Signal.NORMAL || response == Response.KEEP_IDENTITY,
                "DECISION_WITHOUT_SIGNAL");
        require(response != Response.PROMOTE || strategy.channel() != Channel.NONE,
                "PROMOTION_CHANNEL_REQUIRED");

        long essential = (active == MonthlyShopRules.Shock.RENT_RENEWAL ? 2_800 : 2_000)
                + (month == 1 ? 4_000 : 0);
        long standingCost = strategy.monthlyMarketingBudgetMinor();
        boolean guard = game.cashMinor() - essential - standingCost < strategy.minimumReserveMinor();
        if (guard) standingCost = 0;
        long decisionCost = switch (response) {
            case KEEP_IDENTITY -> 0;
            case PROMOTE -> 600;
            case TEMPORARY_PIVOT -> 300;
        };
        require(game.cashMinor() - essential - standingCost - decisionCost >= strategy.minimumReserveMinor()
                || response == Response.KEEP_IDENTITY, "V5_CHOICE_UNAFFORDABLE");

        List<Integer> extraBuyers = new ArrayList<>();
        if (!guard && standingCost > 0) extraBuyers.add(strategy.unitPriceCoins());
        // A signed audience-service fit retains one bounded customer during a market shift.
        if (signal == Signal.MARKET_SHIFT && strategy.audience() == Audience.NIGHT_READERS
                && strategy.servicePromise() == ServicePromise.QUIET) extraBuyers.add(20);
        if (response == Response.PROMOTE) {
            extraBuyers.add(strategy.unitPriceCoins());
            if (extraBuyers.size() < 3) extraBuyers.add(strategy.unitPriceCoins());
        }
        if (response == Response.TEMPORARY_PIVOT && extraBuyers.size() < 3) extraBuyers.add(12);
        if (extraBuyers.size() > 3) extraBuyers = new ArrayList<>(extraBuyers.subList(0, 3));
        int price = response == Response.TEMPORARY_PIVOT ? 12 : strategy.unitPriceCoins();
        int production = signal == Signal.MARKET_SHIFT && response == Response.KEEP_IDENTITY
                ? Math.max(0, strategy.produceUnits() - 1) : strategy.produceUnits();
        MonthlyShopRules.Plan plan = new MonthlyShopRules.Plan(production, price, strategy.minimumReserveMinor());
        MonthlyShopRules.TradingAdjustment adjustment = new MonthlyShopRules.TradingAdjustment(
                standingCost + decisionCost, 0, extraBuyers, signal == Signal.COMPETITOR ? 2 : 0,
                "V5_" + response.name());
        MonthlyShopRules.State next = ledger.advanceMonth(game, plan, null, adjustment);
        boolean failedToOpen = next.failedOpeningMonth() != null;
        return new MonthResult(next, response, failedToOpen ? 0 : standingCost + decisionCost,
                failedToOpen ? 0 : extraBuyers.size(), guard);
    }

    private static void require(boolean condition, String error) {
        if (!condition) throw new IllegalArgumentException(error);
    }
}
