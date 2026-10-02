package com.ai.repo.playground.rules;

import java.util.List;
import org.junit.jupiter.api.Test;
import static com.ai.repo.playground.rules.MonthlyShopRules.*;
import static com.ai.repo.playground.rules.V5ShopRules.*;
import static org.junit.jupiter.api.Assertions.*;

class V5ShopRulesTest {
    private final MonthlyShopRules ledger = new MonthlyShopRules();
    private final V5ShopRules v5 = new V5ShopRules();
    private final Strategy strategy = new Strategy(4, 16, 2_000,
            Audience.COMMUTERS, Channel.FLYERS, ServicePromise.FAST, 300);

    private State beforeMarketMonth() {
        State first = ledger.initialize(12, new Environment(Shock.MARKET_SLOWDOWN, 2, 2));
        return v5.advance(first, strategy, Signal.NORMAL, Response.KEEP_IDENTITY).game();
    }

    private void reconciles(State state) {
        long previous = CAPITAL;
        long cumulative = 0;
        for (MonthlyReport report : state.reports()) {
            assertEquals(previous, report.openingCashMinor());
            assertEquals(report.openingCashMinor() + report.receiptsMinor() - report.paymentsMinor(),
                    report.closingCashMinor());
            assertEquals(report.salesMinor() - report.costOfGoodsSoldMinor() - report.operatingExpenseMinor()
                            + report.liquidationReceiptsMinor() - report.liquidationBookCostMinor(),
                    report.profitMinor());
            cumulative += report.profitMinor();
            assertEquals(cumulative, report.cumulativeProfitMinor());
            previous = report.closingCashMinor();
        }
        assertEquals(state.cashMinor(), previous);
    }

    @Test void sameSignedPlanSignalAndResponseAlwaysProduceSameLedger() {
        State before = beforeMarketMonth();
        MonthResult first = v5.advance(before, strategy, Signal.MARKET_SHIFT, Response.PROMOTE);
        MonthResult replay = v5.advance(before, strategy, Signal.MARKET_SHIFT, Response.PROMOTE);
        assertEquals(first, replay);
        assertEquals(900, first.marketingSpentMinor());
        assertTrue(first.game().reports().get(1).events().contains("V5_PROMOTE"));
        reconciles(first.game());
    }

    @Test void partnersChoicesChangeTheActualMonthRatherThanOnlyTheStory() {
        State before = beforeMarketMonth();
        MonthResult keep = v5.advance(before, strategy, Signal.MARKET_SHIFT, Response.KEEP_IDENTITY);
        MonthResult promote = v5.advance(before, strategy, Signal.MARKET_SHIFT, Response.PROMOTE);
        MonthResult pivot = v5.advance(before, strategy, Signal.MARKET_SHIFT, Response.TEMPORARY_PIVOT);
        MonthlyReport kept = keep.game().reports().get(1);
        MonthlyReport promoted = promote.game().reports().get(1);
        MonthlyReport pivoted = pivot.game().reports().get(1);
        assertNotEquals(kept, promoted);
        assertNotEquals(promoted, pivoted);
        assertTrue(promoted.soldUnits() > kept.soldUnits());
        assertTrue(promoted.operatingExpenseMinor() > kept.operatingExpenseMinor());
        assertNotEquals(promoted.salesMinor(), pivoted.salesMinor());
        reconciles(keep.game());
        reconciles(promote.game());
        reconciles(pivot.game());
    }

    @Test void supplierDelayChangesStockAndTheAgreedResponseChangesRecovery() {
        State start=ledger.initialize(12,new Environment(Shock.NONE,1,12,24));
        State before=v5.advance(start,strategy,Signal.NORMAL,Response.KEEP_IDENTITY,null,true).game();
        MonthResult keep=v5.advance(before,strategy,Signal.SUPPLY_DELAY,Response.KEEP_IDENTITY,null,true);
        MonthResult pivot=v5.advance(before,strategy,Signal.SUPPLY_DELAY,Response.TEMPORARY_PIVOT,null,true);
        MonthlyReport kept=keep.game().reports().get(1),pivoted=pivot.game().reports().get(1);
        assertTrue(kept.events().contains("SUPPLY_DELAY"));
        assertTrue(pivoted.costOfGoodsSoldMinor()>kept.costOfGoodsSoldMinor());
        assertNotEquals(kept.paymentsMinor(),pivoted.paymentsMinor());
        reconciles(keep.game());
        reconciles(pivot.game());
    }

    @Test void audienceAndServiceFitRetainARealBuyerDuringMarketShift() {
        Strategy niche = new Strategy(4, 16, 0, Audience.NIGHT_READERS,
                Channel.NONE, ServicePromise.QUIET, 0);
        Strategy generic = new Strategy(4, 16, 0, Audience.COMMUTERS,
                Channel.NONE, ServicePromise.QUIET, 0);
        State before = ledger.advanceMonth(ledger.initialize(12,
                new Environment(Shock.MARKET_SLOWDOWN, 2, 2)), new Plan(4, 16, 0));
        MonthResult fitted = v5.advance(before, niche, Signal.MARKET_SHIFT, Response.KEEP_IDENTITY);
        MonthResult unfitted = v5.advance(before, generic, Signal.MARKET_SHIFT, Response.KEEP_IDENTITY);
        assertEquals(1, fitted.addedBuyers());
        assertEquals(0, unfitted.addedBuyers());
        assertTrue(fitted.game().reports().get(1).salesMinor() > unfitted.game().reports().get(1).salesMinor());
    }

    @Test void reserveGuardCancelsRoutineMarketingWithoutInventingAChoice() {
        Strategy cautious = new Strategy(4, 16, 13_500,
                Audience.COMMUTERS, Channel.FLYERS, ServicePromise.FAST, 600);
        State start = ledger.initialize(12, new Environment(Shock.NONE, 1, 12));
        MonthResult month = v5.advance(start, cautious, Signal.NORMAL, Response.KEEP_IDENTITY);
        assertTrue(month.budgetGuardTriggered());
        assertEquals(0, month.marketingSpentMinor());
        assertEquals(6_000, month.game().reports().get(0).operatingExpenseMinor());
        assertThrows(IllegalArgumentException.class,
                () -> v5.advance(start, cautious, Signal.COMPETITOR, Response.PROMOTE));
    }

    @Test void invalidSignalStrategyAndUnapprovedFreeformAmountsAreRejected() {
        State before = beforeMarketMonth();
        assertThrows(IllegalArgumentException.class,
                () -> v5.advance(before, strategy, Signal.RENT_RISE, Response.KEEP_IDENTITY));
        assertThrows(IllegalArgumentException.class,
                () -> v5.advance(before, strategy, Signal.NORMAL, Response.PROMOTE));
        assertThrows(IllegalArgumentException.class,
                () -> new Strategy(4, 16, 0, Audience.STUDENTS, Channel.NONE, ServicePromise.COMMUNITY, 300));
        assertThrows(IllegalArgumentException.class,
                () -> new TradingAdjustment(20_000, 0, List.of(), 0, "FREE_MONEY"));
    }

    @Test void existingV4ReducerRemainsExactlyTheSameWithNoAdjustment() {
        State before = beforeMarketMonth();
        Plan old = new Plan(4, 16, 2_000);
        assertEquals(ledger.advanceMonth(before, old),
                ledger.advanceMonth(before, old, null, TradingAdjustment.NONE));
    }

    @Test void newRoomsHaveRepeatableButUnevenFootfallAndBalancedAccounts() {
        Strategy local = new Strategy(4,16,0,Audience.NEIGHBORS,
                Channel.NONE,ServicePromise.COMMUNITY,0);
        State game = ledger.initialize(12,new Environment(Shock.NONE,1,12,18742));
        State replay = game;
        for (int month=1;month<=12;month++) {
            game=v5.advance(game,local,Signal.NORMAL,Response.KEEP_IDENTITY,null,true).game();
            replay=v5.advance(replay,local,Signal.NORMAL,Response.KEEP_IDENTITY,null,true).game();
        }
        assertEquals(game,replay);
        assertEquals(Ending.YEAR_COMPLETE,game.ending());
        assertTrue(game.reports().stream().map(MonthlyReport::salesMinor).distinct().count()>2);
        assertTrue(game.reports().stream().anyMatch(r->r.events().contains("QUIET_STREET")));
        assertTrue(game.reports().stream().anyMatch(r->r.events().contains("LOCAL_RUSH")));
        reconciles(game);
    }

    @Test void newServiceAudienceCanRetainDemandInMarketShift() {
        State first=ledger.initialize(12,new Environment(Shock.MARKET_SLOWDOWN,2,2,919));
        Strategy fitted=new Strategy(4,16,0,Audience.PET_OWNERS,
                Channel.NONE,ServicePromise.COMMUNITY,0);
        Strategy mismatched=new Strategy(4,16,0,Audience.PET_OWNERS,
                Channel.NONE,ServicePromise.QUIET,0);
        State before=v5.advance(first,fitted,Signal.NORMAL,Response.KEEP_IDENTITY,null,true).game();
        MonthResult good=v5.advance(before,fitted,Signal.MARKET_SHIFT,Response.KEEP_IDENTITY,null,true);
        MonthResult bad=v5.advance(before,mismatched,Signal.MARKET_SHIFT,Response.KEEP_IDENTITY,null,true);
        assertTrue(good.addedBuyers()>bad.addedBuyers());
    }

    @Test void signedMarketingDoesNotFlattenNewRoomSales() {
        State game=ledger.initialize(12,new Environment(Shock.NONE,1,12,18742));
        for (int month=1;month<=12;month++)
            game=v5.advance(game,strategy,Signal.NORMAL,Response.KEEP_IDENTITY,null,true).game();
        long minimum=game.reports().stream().mapToLong(MonthlyReport::salesMinor).min().orElseThrow();
        long maximum=game.reports().stream().mapToLong(MonthlyReport::salesMinor).max().orElseThrow();
        assertTrue(maximum-minimum>=3_200);
        reconciles(game);
    }

    @Test void competitorRemovesRealDemandAndTheYearStillEndsAtTwelveMonths() {
        Strategy noCampaign = new Strategy(4, 16, 0, Audience.COMMUTERS,
                Channel.NONE, ServicePromise.FAST, 0);
        State start = ledger.initialize(12, new Environment(Shock.NONE, 1, 12));
        State afterFirst = v5.advance(start, noCampaign, Signal.NORMAL, Response.KEEP_IDENTITY).game();
        State baseline = v5.advance(afterFirst, noCampaign, Signal.NORMAL, Response.KEEP_IDENTITY).game();
        State affected = v5.advance(afterFirst, noCampaign, Signal.COMPETITOR, Response.KEEP_IDENTITY).game();
        assertTrue(affected.reports().get(1).salesMinor() < baseline.reports().get(1).salesMinor());
        assertTrue(affected.reports().get(1).events().contains("V5_KEEP_IDENTITY"));
        for (int month = 3; month <= 12; month++)
            affected = v5.advance(affected, noCampaign, Signal.NORMAL, Response.KEEP_IDENTITY).game();
        assertEquals(12, affected.operatedMonths());
        assertEquals(Ending.YEAR_COMPLETE, affected.ending());
        reconciles(affected);
    }
}
