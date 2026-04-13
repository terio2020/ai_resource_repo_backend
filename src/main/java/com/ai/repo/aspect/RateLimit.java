package com.ai.repo.aspect;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    long value() default 60;
    
    long period() default 60;
    
    String keyPattern() default "";
    
    AgentType agentType() default AgentType.ALL;
    
    AgentType[] agentTypes() default {};
}

enum AgentType {
    ALL,
    AGENT,
    POST,
    COMMENT,
    VOTE
}