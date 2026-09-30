package com.ai.repo.playground.rules;

import static org.junit.jupiter.api.Assertions.*;
import static com.ai.repo.playground.rules.MonthlyShopRules.*;
import static com.ai.repo.playground.rules.V5ShopRules.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class V5FranchiseOfferTest {
    @Test void termsAndQualityAreDrawnOnceAndOnlyVerifiedTermsArePublic() {
        var first=V5FranchiseOffer.draw("room-1:pitch",3,new Random(42));
        var replay=V5FranchiseOffer.draw("room-1:pitch",3,new Random(42));
        assertEquals(first,replay);
        assertEquals("FRANCHISE_SALESPERSON",first.publicTerms().get("npcRole"));
        assertFalse(first.publicTerms().containsKey("support"));
        assertEquals(6_000L,first.publicTerms().get("entryFeeMinor"));
        assertEquals(first.clue(V5FranchiseOffer.Investigation.STORES),
                replay.clue(V5FranchiseOffer.Investigation.STORES));
    }

    @Test void investigationDifferentiatesSupportWithoutChangingTheContract() {
        var good=new V5FranchiseOffer("good",1,3,6_000,900,200,3_000,
                V5FranchiseOffer.Support.DELIVERED);
        var bad=new V5FranchiseOffer("bad",1,3,6_000,900,200,3_000,
                V5FranchiseOffer.Support.ABSENT);
        assertEquals(good.publicTerms().get("entryFeeMinor"),bad.publicTerms().get("entryFeeMinor"));
        assertNotEquals(good.clue(V5FranchiseOffer.Investigation.STORES),
                bad.clue(V5FranchiseOffer.Investigation.STORES));
        assertEquals(good.clue(V5FranchiseOffer.Investigation.TERMS),
                bad.clue(V5FranchiseOffer.Investigation.TERMS));
    }

    @Test void signingCreatesEntryAndRecurringObligationsRatherThanFreeMoney() {
        var offer=new V5FranchiseOffer("costly",1,3,6_000,900,200,3_000,
                V5FranchiseOffer.Support.ABSENT);
        assertEquals(0,offer.entryCost(2));
        assertEquals(6_000,offer.entryCost(3));
        assertEquals(0,offer.entryCost(4));
        assertEquals(900,offer.monthlyCost(4));
        assertEquals(200,offer.unitPremium(4));
        assertEquals(0,offer.supportBuyers(4));
        assertEquals("FRANCHISE_SUPPORT_ABSENT",offer.resultEvent(4));
    }

    @Test void impossibleTermsAndFutureEvidenceAreRejected() {
        assertThrows(IllegalArgumentException.class,()->V5FranchiseOffer.draw("offer",5,new Random(1)));
        assertThrows(IllegalArgumentException.class,()->new V5FranchiseOffer("offer",1,2,
                30_000,900,200,3_000,V5FranchiseOffer.Support.ABSENT));
        var offer=V5FranchiseOffer.draw("offer",2,new Random(1));
        assertThrows(IllegalArgumentException.class,()->offer.resultEvent(1));
    }

    @Test void signedOfferChangesTheActualLedgerAndWeakSupportDoesNotAddFreeBuyers() {
        var ledger=new MonthlyShopRules();
        var rules=new V5ShopRules();
        var strategy=new Strategy(4,16,0,Audience.COMMUTERS,Channel.NONE,ServicePromise.FAST,0);
        var start=ledger.initialize(12,new Environment(Shock.NONE,1,12));
        var afterOne=rules.advance(start,strategy,Signal.NORMAL,Response.KEEP_IDENTITY).game();
        var afterTwo=rules.advance(afterOne,strategy,Signal.COMPETITOR,Response.KEEP_IDENTITY).game();
        var bad=new V5FranchiseOffer("bad",1,3,6_000,900,200,3_000,V5FranchiseOffer.Support.ABSENT);
        var reject=rules.advance(afterTwo,strategy,Signal.NORMAL,Response.KEEP_IDENTITY).game();
        var sign=rules.advance(afterTwo,strategy,Signal.NORMAL,Response.KEEP_IDENTITY,bad).game();
        MonthlyReport refused=reject.reports().get(2),joined=sign.reports().get(2);
        assertTrue(joined.operatingExpenseMinor()>refused.operatingExpenseMinor());
        assertTrue(joined.paymentsMinor()>refused.paymentsMinor());
        assertTrue(joined.events().contains("FRANCHISE_SIGNED"));
        assertFalse(refused.events().contains("FRANCHISE_SIGNED"));
        assertEquals(0,bad.supportBuyers(4));
        var next=rules.advance(sign,strategy,Signal.NORMAL,Response.KEEP_IDENTITY,bad).game();
        assertTrue(next.reports().get(3).events().contains("FRANCHISE_SUPPORT_ABSENT"));
        assertTrue(next.reports().get(3).profitMinor()<
                rules.advance(reject,strategy,Signal.NORMAL,Response.KEEP_IDENTITY).game().reports().get(3).profitMinor());
    }

    @Test void fixedSeedSampleShowsHigherFailureRiskThanRejectingThePitch() {
        int signedFailures=0,rejectedFailures=0;
        for (int seed=0;seed<200;seed++) {
            if (simulate(seed,true).ending()==Ending.BUSINESS_FAILURE) signedFailures++;
            if (simulate(seed,false).ending()==Ending.BUSINESS_FAILURE) rejectedFailures++;
        }
        assertTrue(signedFailures>=120 && signedFailures<=150,
                "Fixed-seed blind-sign failure count: "+signedFailures);
        assertTrue(signedFailures>rejectedFailures,
                "The same seeds must remain safer when both Agents reject the pitch");
    }

    private State simulate(int seed,boolean signed) {
        var ledger=new MonthlyShopRules();
        var rules=new V5ShopRules();
        var strategy=new Strategy(4,16,2_000,Audience.NIGHT_READERS,Channel.FLYERS,ServicePromise.QUIET,300);
        State state=ledger.initialize(12,ledger.drawEnvironment(12,new Random(seed+10_000)));
        var offer=V5FranchiseOffer.draw("seed-"+seed,3,new Random(seed+30_000));
        while (state.ending()==Ending.RUNNING) {
            int month=state.operatedMonths()+1;
            var env=state.environment();
            Signal signal=month<env.fromMonth() || month>env.throughMonth()?Signal.NORMAL:switch(env.shock()) {
                case NONE -> Signal.NORMAL;
                case MARKET_SLOWDOWN -> Signal.MARKET_SHIFT;
                case MATERIAL_SURGE -> Signal.MATERIAL_SURGE;
                case RENT_RENEWAL -> Signal.RENT_RISE;
                case PACKAGING_RULE -> Signal.PACKAGING_CHANGE;
                case POWER_OUTAGE -> Signal.POWER_OUTAGE;
            };
            state=rules.advance(state,strategy,signal,Response.KEEP_IDENTITY,
                    signed && month>=3?offer:null).game();
        }
        return state;
    }
}
