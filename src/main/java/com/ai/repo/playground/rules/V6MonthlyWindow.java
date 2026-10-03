package com.ai.repo.playground.rules;

import java.time.Instant;

/** Persisted two-position negotiation. The second position is sealed until both exist. */
public record V6MonthlyWindow(String triggerEventId,String planVersion,int month,
        long firstAgentId,long secondAgentId,Instant deadline,V6ConflictRules.Kind conflict,
        Phase phase,String firstPosition,String secondPosition,String counter,
        String effectiveChoice,Resolution resolution) {
    public enum Phase { AWAIT_FIRST, AWAIT_SECOND, AWAIT_COUNTER, AWAIT_FINAL, CLOSED }
    public enum Resolution { INDEPENDENT_CONSENSUS, COUNTER_ACCEPTED, DECLINED,
        DEADLINE_FALLBACK, BUDGET_FALLBACK, MODEL_FAILURE_FALLBACK }

    public V6MonthlyWindow {
        require(triggerEventId!=null && triggerEventId.matches("[A-Za-z0-9:_-]{1,100}")
                && planVersion!=null && planVersion.matches("[A-Za-z0-9:_-]{1,100}")
                && month>=1 && month<=12 && firstAgentId>0 && secondAgentId>0
                && firstAgentId!=secondAgentId && deadline!=null && conflict!=null && phase!=null,
                "INVALID_V6_WINDOW");
        if (firstPosition!=null) option(conflict,firstPosition);
        if (secondPosition!=null) option(conflict,secondPosition);
        if (counter!=null) option(conflict,counter);
        if (effectiveChoice!=null) option(conflict,effectiveChoice);
        switch (phase) {
            case AWAIT_FIRST -> require(firstPosition==null && secondPosition==null && counter==null
                    && effectiveChoice==null && resolution==null,"INVALID_V6_WINDOW");
            case AWAIT_SECOND -> require(firstPosition!=null && secondPosition==null && counter==null
                    && effectiveChoice==null && resolution==null,"INVALID_V6_WINDOW");
            case AWAIT_COUNTER -> require(firstPosition!=null && secondPosition!=null
                    && !firstPosition.equals(secondPosition) && counter==null
                    && effectiveChoice==null && resolution==null,"INVALID_V6_WINDOW");
            case AWAIT_FINAL -> require(firstPosition!=null && secondPosition!=null && counter!=null
                    && !firstPosition.equals(counter) && effectiveChoice==null && resolution==null,
                    "INVALID_V6_WINDOW");
            case CLOSED -> {
                require(resolution!=null,"INVALID_V6_WINDOW");
                if (resolution==Resolution.INDEPENDENT_CONSENSUS)
                    require(firstPosition!=null && firstPosition.equals(secondPosition) && counter==null
                            && firstPosition.equals(effectiveChoice),"INVALID_V6_WINDOW");
                else if (resolution==Resolution.COUNTER_ACCEPTED)
                    require(firstPosition!=null && secondPosition!=null && counter!=null
                            && counter.equals(effectiveChoice),"INVALID_V6_WINDOW");
                else require(effectiveChoice==null,"INVALID_V6_WINDOW");
            }
        }
    }

    public static V6MonthlyWindow open(String triggerEventId,String planVersion,int month,
            long firstAgentId,long secondAgentId,Instant deadline,V6ConflictRules.Kind conflict) {
        return new V6MonthlyWindow(triggerEventId,planVersion,month,firstAgentId,secondAgentId,
                deadline,conflict,Phase.AWAIT_FIRST,null,null,null,null,null);
    }
    public long currentActor() {
        return switch (phase) {
            case AWAIT_FIRST, AWAIT_COUNTER -> firstAgentId;
            case AWAIT_SECOND, AWAIT_FINAL -> secondAgentId;
            case CLOSED -> throw new IllegalStateException("V6_WINDOW_CLOSED");
        };
    }
    public V6MonthlyWindow position(long actor,String version,String choice,Instant now) {
        require(phase==Phase.AWAIT_FIRST || phase==Phase.AWAIT_SECOND,"V6_ACTION_NOT_ALLOWED");
        action(actor,version,now); option(conflict,choice);
        if (phase==Phase.AWAIT_FIRST)
            return copy(Phase.AWAIT_SECOND,choice,null,null,null,null);
        if (firstPosition.equals(choice))
            return copy(Phase.CLOSED,firstPosition,choice,null,choice,Resolution.INDEPENDENT_CONSENSUS);
        return copy(Phase.AWAIT_COUNTER,firstPosition,choice,null,null,null);
    }
    public V6MonthlyWindow counter(long actor,String version,String choice,
            String keptFromPartner,String concededOwnPoint,Instant now) {
        require(phase==Phase.AWAIT_COUNTER,"V6_ACTION_NOT_ALLOWED"); action(actor,version,now);
        option(conflict,choice);
        require(secondPosition.equals(keptFromPartner) && firstPosition.equals(concededOwnPoint)
                && !firstPosition.equals(choice),"INVALID_V6_CONCESSION");
        return copy(Phase.AWAIT_FINAL,firstPosition,secondPosition,choice,null,null);
    }
    public V6MonthlyWindow accept(long actor,String version,Instant now) {
        require(phase==Phase.AWAIT_FINAL,"V6_ACTION_NOT_ALLOWED"); action(actor,version,now);
        return copy(Phase.CLOSED,firstPosition,secondPosition,counter,counter,Resolution.COUNTER_ACCEPTED);
    }
    public V6MonthlyWindow decline(long actor,String version,Instant now) {
        require(phase==Phase.AWAIT_COUNTER || phase==Phase.AWAIT_FINAL,"V6_ACTION_NOT_ALLOWED");
        action(actor,version,now);
        return copy(Phase.CLOSED,firstPosition,secondPosition,counter,null,Resolution.DECLINED);
    }
    public V6MonthlyWindow expire(Instant now) {
        require(phase!=Phase.CLOSED && now!=null && !now.isBefore(deadline),"V6_WINDOW_NOT_EXPIRED");
        return miss(Resolution.DEADLINE_FALLBACK);
    }
    public V6MonthlyWindow miss(Resolution why) {
        require(phase!=Phase.CLOSED && (why==Resolution.DEADLINE_FALLBACK
                || why==Resolution.BUDGET_FALLBACK || why==Resolution.MODEL_FAILURE_FALLBACK),
                "INVALID_V6_FALLBACK");
        return copy(Phase.CLOSED,firstPosition,secondPosition,counter,null,why);
    }
    private V6MonthlyWindow copy(Phase next,String first,String second,String counterChoice,
            String effective,Resolution why) {
        return new V6MonthlyWindow(triggerEventId,planVersion,month,firstAgentId,secondAgentId,
                deadline,conflict,next,first,second,counterChoice,effective,why);
    }
    private void action(long actor,String version,Instant now) {
        require(actor==currentActor(),"V6_ACTION_NOT_ALLOWED");
        require(planVersion.equals(version),"STALE_V6_PLAN");
        require(now!=null && now.isBefore(deadline),"V6_WINDOW_EXPIRED");
    }
    private static void option(V6ConflictRules.Kind kind,String code) {
        require(code!=null && kind.options().stream().anyMatch(value->value.code().equals(code)),
                "INVALID_V6_CHOICE");
    }
    private static void require(boolean valid,String error) {
        if (!valid) throw new IllegalArgumentException(error);
    }
}
