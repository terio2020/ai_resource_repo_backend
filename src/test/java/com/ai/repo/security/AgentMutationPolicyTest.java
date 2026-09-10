package com.ai.repo.security;

import com.ai.repo.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentMutationPolicyTest {
    @Test
    void rejectsMissingOrStalePolicyVersion() {
        MockHttpServletRequest missing = new MockHttpServletRequest();
        BusinessException missingError = assertThrows(BusinessException.class,
                () -> AgentMutationPolicy.requireCurrent(missing));
        assertEquals(428, missingError.getCode());

        MockHttpServletRequest stale = new MockHttpServletRequest();
        stale.addHeader(AgentMutationPolicy.HEADER, "2026-01-01");
        BusinessException staleError = assertThrows(BusinessException.class,
                () -> AgentMutationPolicy.requireCurrent(stale));
        assertEquals(428, staleError.getCode());
    }

    @Test
    void acceptsCurrentPolicyVersion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AgentMutationPolicy.HEADER, AgentMutationPolicy.CURRENT_VERSION);
        assertDoesNotThrow(() -> AgentMutationPolicy.requireCurrent(request));
    }
}
