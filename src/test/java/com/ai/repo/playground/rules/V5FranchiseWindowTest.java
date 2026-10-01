package com.ai.repo.playground.rules;

import static org.junit.jupiter.api.Assertions.*;
import static com.ai.repo.playground.rules.V5FranchiseWindow.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class V5FranchiseWindowTest {
    private final V5FranchiseOffer offer=new V5FranchiseOffer("room-1:pitch",1,3,
            6_000,900,200,3_000,V5FranchiseOffer.Support.ABSENT);
    private final Instant now=Instant.parse("2026-10-01T00:00:00Z");

    private V5FranchiseWindow open() {
        return V5FranchiseWindow.open(offer,101,202,now.plusSeconds(900));
    }

    @Test void oneProbePerAgentAndBothSignaturesAreRequiredBeforeAnyCost() {
        var window=open().investigate(101,offer.offerId(),1,
                V5FranchiseOffer.Investigation.STORES,now);
        assertEquals(1,window.investigations().size());
        assertThrows(IllegalArgumentException.class,()->window.investigate(101,offer.offerId(),1,
                V5FranchiseOffer.Investigation.SUPPLY,now));
        var proposed=window.propose(101,offer.offerId(),1,Decision.SIGN,now);
        assertEquals(Phase.AWAIT_REPLY,proposed.phase());
        assertNull(proposed.resolution());
        assertThrows(IllegalArgumentException.class,()->proposed.accept(101,offer.offerId(),1,now));
        var accepted=proposed.investigate(202,offer.offerId(),1,
                V5FranchiseOffer.Investigation.TERMS,now).accept(202,offer.offerId(),1,now);
        assertEquals(Resolution.SIGNED,accepted.resolution());
        assertThrows(IllegalArgumentException.class,()->accepted.accept(202,offer.offerId(),1,now));
    }

    @Test void counterOrDeclinePreservesOriginalShopAndNeverSignsUnilaterally() {
        var first=open().propose(101,offer.offerId(),1,Decision.SIGN,now);
        var counter=first.counter(202,offer.offerId(),1,Decision.REJECT,now);
        assertEquals(Resolution.REJECTED,counter.accept(101,offer.offerId(),1,now).resolution());
        assertEquals(Resolution.REJECTED,first.decline(202,offer.offerId(),1,now).resolution());
        assertThrows(IllegalArgumentException.class,()->counter.counter(202,offer.offerId(),1,Decision.SIGN,now));
    }

    @Test void staleTermsDeadlineAndBudgetFailClosed() {
        var window=open();
        assertThrows(IllegalArgumentException.class,()->window.propose(101,offer.offerId(),2,Decision.SIGN,now));
        assertThrows(IllegalArgumentException.class,()->window.propose(101,offer.offerId(),1,
                Decision.SIGN,now.plusSeconds(901)));
        assertEquals(Resolution.DEADLINE_FALLBACK,
                window.expire(now.plusSeconds(900)).resolution());
        assertEquals(Resolution.BUDGET_FALLBACK,
                window.miss(Resolution.BUDGET_FALLBACK).resolution());
        assertThrows(IllegalArgumentException.class,()->window.miss(Resolution.SIGNED));
        assertThrows(IllegalArgumentException.class,()->new V5FranchiseWindow(offer.offerId(),1,
                101,202,now.plusSeconds(900),Phase.CLOSED,java.util.Map.of(),null,null,
                Resolution.SIGNED));
    }
}
