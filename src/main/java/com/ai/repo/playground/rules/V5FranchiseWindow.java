package com.ai.repo.playground.rules;

import java.time.Instant;
import java.util.Map;

/** Finite two-Agent consent for one persisted franchise offer. No NPC or model can sign for an Agent. */
public record V5FranchiseWindow(String offerId, int offerVersion, long proposerAgentId,
        long partnerAgentId, Instant deadline, Phase phase,
        Map<Long,V5FranchiseOffer.Investigation> investigations,
        Decision proposal, Decision counterProposal, Resolution resolution) {
    public enum Phase { AWAIT_PROPOSAL, AWAIT_REPLY, AWAIT_COUNTER_REPLY, CLOSED }
    public enum Decision { SIGN, REJECT }
    public enum Resolution { SIGNED, REJECTED, DEADLINE_FALLBACK, BUDGET_FALLBACK, MODEL_FAILURE_FALLBACK }

    public V5FranchiseWindow {
        require(offerId!=null && offerId.matches("[A-Za-z0-9:_-]{1,100}") && offerVersion==1
                && proposerAgentId>0 && partnerAgentId>0 && proposerAgentId!=partnerAgentId
                && deadline!=null && phase!=null && investigations!=null,
                "INVALID_FRANCHISE_WINDOW");
        investigations=Map.copyOf(investigations);
        require(investigations.keySet().stream().allMatch(id -> id==proposerAgentId || id==partnerAgentId)
                && investigations.values().stream().noneMatch(value -> value==null),"INVALID_FRANCHISE_WINDOW");
        switch (phase) {
            case AWAIT_PROPOSAL -> require(proposal==null && counterProposal==null && resolution==null,
                    "INVALID_FRANCHISE_WINDOW");
            case AWAIT_REPLY -> require(proposal!=null && counterProposal==null && resolution==null,
                    "INVALID_FRANCHISE_WINDOW");
            case AWAIT_COUNTER_REPLY -> require(proposal!=null && counterProposal!=null
                    && counterProposal!=proposal && resolution==null,"INVALID_FRANCHISE_WINDOW");
            case CLOSED -> {
                require(resolution!=null,"INVALID_FRANCHISE_WINDOW");
                if (resolution==Resolution.SIGNED)
                    require(proposal==Decision.SIGN && counterProposal==null
                            || proposal==Decision.REJECT && counterProposal==Decision.SIGN,
                            "INVALID_FRANCHISE_WINDOW");
            }
        }
    }

    public static V5FranchiseWindow open(V5FranchiseOffer offer,long proposer,long partner,Instant deadline) {
        require(offer!=null,"INVALID_FRANCHISE_OFFER");
        return new V5FranchiseWindow(offer.offerId(),offer.version(),proposer,partner,deadline,
                Phase.AWAIT_PROPOSAL,Map.of(),null,null,null);
    }

    public V5FranchiseWindow investigate(long actor,String id,int version,
            V5FranchiseOffer.Investigation kind,Instant now) {
        action(actor,id,version,now);
        require(kind!=null && !investigations.containsKey(actor),"FRANCHISE_INVESTIGATION_EXHAUSTED");
        var next=new java.util.LinkedHashMap<>(investigations);next.put(actor,kind);
        return new V5FranchiseWindow(offerId,offerVersion,proposerAgentId,partnerAgentId,deadline,
                phase,next,proposal,counterProposal,resolution);
    }

    public V5FranchiseWindow propose(long actor,String id,int version,Decision choice,Instant now) {
        action(actor,id,version,now);
        require(phase==Phase.AWAIT_PROPOSAL && actor==proposerAgentId && choice!=null,
                "FRANCHISE_ACTION_NOT_ALLOWED");
        return next(Phase.AWAIT_REPLY,choice,null,null);
    }

    public V5FranchiseWindow counter(long actor,String id,int version,Decision choice,Instant now) {
        action(actor,id,version,now);
        require(phase==Phase.AWAIT_REPLY && actor==partnerAgentId
                && choice!=null && choice!=proposal,"FRANCHISE_ACTION_NOT_ALLOWED");
        return next(Phase.AWAIT_COUNTER_REPLY,proposal,choice,null);
    }

    public V5FranchiseWindow accept(long actor,String id,int version,Instant now) {
        action(actor,id,version,now);
        require((phase==Phase.AWAIT_REPLY && actor==partnerAgentId)
                || (phase==Phase.AWAIT_COUNTER_REPLY && actor==proposerAgentId),
                "FRANCHISE_ACTION_NOT_ALLOWED");
        Decision agreed=phase==Phase.AWAIT_REPLY?proposal:counterProposal;
        return next(Phase.CLOSED,proposal,counterProposal,
                agreed==Decision.SIGN?Resolution.SIGNED:Resolution.REJECTED);
    }

    public V5FranchiseWindow decline(long actor,String id,int version,Instant now) {
        action(actor,id,version,now);
        require((phase==Phase.AWAIT_REPLY && actor==partnerAgentId)
                || (phase==Phase.AWAIT_COUNTER_REPLY && actor==proposerAgentId),
                "FRANCHISE_ACTION_NOT_ALLOWED");
        return next(Phase.CLOSED,proposal,counterProposal,Resolution.REJECTED);
    }

    public V5FranchiseWindow miss(Resolution why) {
        require(phase!=Phase.CLOSED && (why==Resolution.DEADLINE_FALLBACK
                || why==Resolution.BUDGET_FALLBACK || why==Resolution.MODEL_FAILURE_FALLBACK),
                "INVALID_FRANCHISE_FALLBACK");
        return next(Phase.CLOSED,proposal,counterProposal,why);
    }

    public V5FranchiseWindow expire(Instant now) {
        require(now!=null && !now.isBefore(deadline),"FRANCHISE_WINDOW_NOT_EXPIRED");
        return miss(Resolution.DEADLINE_FALLBACK);
    }

    private V5FranchiseWindow next(Phase nextPhase,Decision nextProposal,
            Decision nextCounter,Resolution nextResolution) {
        return new V5FranchiseWindow(offerId,offerVersion,proposerAgentId,partnerAgentId,deadline,
                nextPhase,investigations,nextProposal,nextCounter,nextResolution);
    }
    private void action(long actor,String id,int version,Instant now) {
        require(phase!=Phase.CLOSED && (phase==Phase.AWAIT_PROPOSAL?actor==proposerAgentId:
                phase==Phase.AWAIT_REPLY?actor==partnerAgentId:actor==proposerAgentId),
                "FRANCHISE_ACTION_NOT_ALLOWED");
        require(offerId.equals(id) && offerVersion==version,"STALE_FRANCHISE_OFFER");
        require(now!=null && now.isBefore(deadline),"FRANCHISE_WINDOW_EXPIRED");
    }
    private static void require(boolean okay,String code) {
        if (!okay) throw new IllegalArgumentException(code);
    }
}
