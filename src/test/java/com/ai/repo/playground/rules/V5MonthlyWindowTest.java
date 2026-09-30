package com.ai.repo.playground.rules;

import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import static com.ai.repo.playground.rules.V5MonthlyWindow.*;
import static com.ai.repo.playground.rules.V5ShopRules.Response.*;
import static org.junit.jupiter.api.Assertions.*;

class V5MonthlyWindowTest {
    private final Instant deadline = Instant.parse("2026-09-30T12:00:00Z");
    private final Instant before = deadline.minusSeconds(10);

    private V5MonthlyWindow opened() {
        return V5MonthlyWindow.open("game:42", "plan:7", 2, 11, 22, deadline);
    }

    @Test void oneAgentCannotCommitAChangeWithoutPartnerApproval() {
        V5MonthlyWindow proposed = opened().propose(11, "plan:7", PROMOTE, before);
        assertEquals(Phase.AWAIT_REPLY, proposed.phase());
        assertNull(proposed.effectiveResponse());
        assertThrows(IllegalArgumentException.class,
                () -> proposed.accept(11, "plan:7", before));
        V5MonthlyWindow approved = proposed.accept(22, "plan:7", before);
        assertEquals(PROMOTE, approved.effectiveResponse());
        assertEquals(Resolution.PARTNERS_APPROVED, approved.resolution());
        assertThrows(IllegalArgumentException.class,
                () -> approved.accept(22, "plan:7", before));
    }

    @Test void oneBoundedCounterNeedsOriginalAgentToApproveTheSamePlan() {
        V5MonthlyWindow proposed = opened().propose(11, "plan:7", PROMOTE, before);
        V5MonthlyWindow counter = proposed.counter(22, "plan:7", TEMPORARY_PIVOT, before);
        assertEquals(Phase.AWAIT_COUNTER_REPLY, counter.phase());
        assertNull(counter.effectiveResponse());
        assertThrows(IllegalArgumentException.class,
                () -> counter.counter(22, "plan:7", KEEP_IDENTITY, before));
        assertThrows(IllegalArgumentException.class,
                () -> counter.accept(11, "plan:8", before));
        V5MonthlyWindow approved = counter.accept(11, "plan:7", before);
        assertEquals(TEMPORARY_PIVOT, approved.effectiveResponse());
    }

    @Test void declinedOrExpiredWindowUsesVisibleFallbackWithoutInventingConsent() {
        V5MonthlyWindow proposed = opened().propose(11, "plan:7", PROMOTE, before);
        V5MonthlyWindow declined = proposed.decline(22, "plan:7", before);
        assertEquals(KEEP_IDENTITY, declined.effectiveResponse());
        assertEquals(Resolution.DECLINED, declined.resolution());
        V5MonthlyWindow expired = proposed.expire(deadline);
        assertEquals(KEEP_IDENTITY, expired.effectiveResponse());
        assertEquals(Resolution.DEADLINE_FALLBACK, expired.resolution());
        assertEquals(PROMOTE, expired.proposal()); // The real proposal remains traceable.
        assertThrows(IllegalArgumentException.class, () -> opened().expire(before));
    }

    @Test void wrongActorOldPlanAndDeadlineAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> opened().propose(22, "plan:7", PROMOTE, before));
        assertThrows(IllegalArgumentException.class,
                () -> opened().propose(11, "plan:6", PROMOTE, before));
        assertThrows(IllegalArgumentException.class,
                () -> opened().propose(11, "plan:7", PROMOTE, deadline));
        assertThrows(IllegalArgumentException.class,
                () -> opened().propose(11, "plan:7", null, before));
        assertThrows(IllegalArgumentException.class,
                () -> opened().propose(11, "plan:7", PROMOTE, before)
                        .counter(22, "plan:7", PROMOTE, before));
        assertThrows(IllegalArgumentException.class,
                () -> new V5MonthlyWindow("game:42", "plan:7", 2, 11, 22, deadline,
                        Phase.CLOSED, null, null, PROMOTE, Resolution.PARTNERS_APPROVED));
    }

    @Test void persistedWindowRoundTripsWithoutInventingAnApproval() throws Exception {
        V5MonthlyWindow pending = opened().propose(11, "plan:7", PROMOTE, before);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        V5MonthlyWindow restored = mapper.readValue(mapper.writeValueAsString(pending),
                V5MonthlyWindow.class);
        assertEquals(pending, restored);
        assertNull(restored.effectiveResponse());
        assertEquals(Resolution.DEADLINE_FALLBACK, restored.expire(deadline).resolution());
    }
}
