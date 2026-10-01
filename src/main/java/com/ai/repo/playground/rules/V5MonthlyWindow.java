package com.ai.repo.playground.rules;

import java.time.Instant;

/** Bounded two-Agent approval for a single persisted virtual-month signal.
 * This reducer makes no model calls and never generates an Agent's public words.
 */
public record V5MonthlyWindow(String triggerEventId, String planVersion, int month,
                              long proposerAgentId, long partnerAgentId, Instant deadline,
                              Phase phase, V5ShopRules.Response proposal,
                              V5ShopRules.Response counterProposal,
                              V5ShopRules.Response effectiveResponse, Resolution resolution) {
    public enum Phase { AWAIT_PROPOSAL, AWAIT_REPLY, AWAIT_COUNTER_REPLY, CLOSED }
    public enum Resolution { PARTNERS_APPROVED, DECLINED, DEADLINE_FALLBACK, BUDGET_FALLBACK, MODEL_FAILURE_FALLBACK }

    public V5MonthlyWindow {
        require(triggerEventId != null && triggerEventId.matches("[A-Za-z0-9:_-]{1,100}")
                && planVersion != null && planVersion.matches("[A-Za-z0-9:_-]{1,100}")
                && month >= 1 && month <= 12 && proposerAgentId > 0 && partnerAgentId > 0
                && proposerAgentId != partnerAgentId && deadline != null && phase != null,
                "INVALID_V5_WINDOW");
        switch (phase) {
            case AWAIT_PROPOSAL -> require(proposal == null && counterProposal == null
                    && effectiveResponse == null && resolution == null, "INVALID_V5_WINDOW");
            case AWAIT_REPLY -> require(proposal != null && counterProposal == null
                    && effectiveResponse == null && resolution == null, "INVALID_V5_WINDOW");
            case AWAIT_COUNTER_REPLY -> require(proposal != null && counterProposal != null
                    && counterProposal != proposal && effectiveResponse == null && resolution == null,
                    "INVALID_V5_WINDOW");
            case CLOSED -> {
                require(effectiveResponse != null && resolution != null, "INVALID_V5_WINDOW");
                if (resolution == Resolution.PARTNERS_APPROVED)
                    require(proposal != null && effectiveResponse ==
                            (counterProposal == null ? proposal : counterProposal), "INVALID_V5_WINDOW");
                else require(effectiveResponse == V5ShopRules.Response.KEEP_IDENTITY,
                        "INVALID_V5_WINDOW");
            }
        }
    }

    public static V5MonthlyWindow open(String triggerEventId, String planVersion, int month,
                                       long proposerAgentId, long partnerAgentId, Instant deadline) {
        return new V5MonthlyWindow(triggerEventId, planVersion, month, proposerAgentId, partnerAgentId,
                deadline, Phase.AWAIT_PROPOSAL, null, null, null, null);
    }

    public V5MonthlyWindow propose(long actor, String expectedPlanVersion,
                                   V5ShopRules.Response choice, Instant now) {
        action(actor, proposerAgentId, expectedPlanVersion, Phase.AWAIT_PROPOSAL, now);
        require(choice != null, "INVALID_V5_CHOICE");
        return new V5MonthlyWindow(triggerEventId, planVersion, month, proposerAgentId, partnerAgentId,
                deadline, Phase.AWAIT_REPLY, choice, null, null, null);
    }

    public V5MonthlyWindow accept(long actor, String expectedPlanVersion, Instant now) {
        Phase expected = phase == Phase.AWAIT_REPLY ? Phase.AWAIT_REPLY : Phase.AWAIT_COUNTER_REPLY;
        long expectedActor = expected == Phase.AWAIT_REPLY ? partnerAgentId : proposerAgentId;
        action(actor, expectedActor, expectedPlanVersion, expected, now);
        V5ShopRules.Response agreed = expected == Phase.AWAIT_REPLY ? proposal : counterProposal;
        require(agreed != null, "INVALID_V5_WINDOW");
        return close(agreed, Resolution.PARTNERS_APPROVED);
    }

    public V5MonthlyWindow counter(long actor, String expectedPlanVersion,
                                   V5ShopRules.Response choice, Instant now) {
        action(actor, partnerAgentId, expectedPlanVersion, Phase.AWAIT_REPLY, now);
        require(choice != null && choice != proposal, "UNCHANGED_V5_COUNTER");
        return new V5MonthlyWindow(triggerEventId, planVersion, month, proposerAgentId, partnerAgentId,
                deadline, Phase.AWAIT_COUNTER_REPLY, proposal, choice, null, null);
    }

    public V5MonthlyWindow decline(long actor, String expectedPlanVersion, Instant now) {
        Phase expected = phase == Phase.AWAIT_REPLY ? Phase.AWAIT_REPLY : Phase.AWAIT_COUNTER_REPLY;
        long expectedActor = expected == Phase.AWAIT_REPLY ? partnerAgentId : proposerAgentId;
        action(actor, expectedActor, expectedPlanVersion, expected, now);
        return close(V5ShopRules.Response.KEEP_IDENTITY, Resolution.DECLINED);
    }

    public V5MonthlyWindow expire(Instant now) {
        require(phase != Phase.CLOSED && now != null && !now.isBefore(deadline),
                "V5_WINDOW_NOT_EXPIRED");
        return close(V5ShopRules.Response.KEEP_IDENTITY, Resolution.DEADLINE_FALLBACK);
    }

    /** Server-only fallback when no legal task can be issued; never records Agent consent. */
    public V5MonthlyWindow miss(Resolution why) {
        require(phase != Phase.CLOSED && (why == Resolution.DEADLINE_FALLBACK
                || why == Resolution.BUDGET_FALLBACK
                || why == Resolution.MODEL_FAILURE_FALLBACK), "INVALID_V5_FALLBACK");
        return close(V5ShopRules.Response.KEEP_IDENTITY, why);
    }

    private V5MonthlyWindow close(V5ShopRules.Response effective, Resolution why) {
        return new V5MonthlyWindow(triggerEventId, planVersion, month, proposerAgentId, partnerAgentId,
                deadline, Phase.CLOSED, proposal, counterProposal, effective, why);
    }

    private void action(long actor, long expectedActor, String expectedVersion, Phase expectedPhase,
                        Instant now) {
        require(phase == expectedPhase && actor == expectedActor, "V5_ACTION_NOT_ALLOWED");
        require(planVersion.equals(expectedVersion), "STALE_V5_PLAN");
        require(now != null && now.isBefore(deadline), "V5_WINDOW_EXPIRED");
    }

    private static void require(boolean condition, String error) {
        if (!condition) throw new IllegalArgumentException(error);
    }
}
