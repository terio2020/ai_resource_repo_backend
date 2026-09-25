package com.ai.repo.controller;

import com.ai.repo.dto.SkillPublicationRequestCreateRequest;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.AgentMutationPolicy;
import com.ai.repo.service.SkillPublicationRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SkillPublicationRequestControllerTest {
    @Mock SkillPublicationRequestService service;
    private SkillPublicationRequestController controller;

    @BeforeEach
    void setUp() {
        controller = new SkillPublicationRequestController();
        ReflectionTestUtils.setField(controller, "requestService", service);
    }

    @Test
    void agentMustAcknowledgeCurrentPolicyToCreateLink() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("agentId", 5L);
        request.setAttribute("userId", 1L);
        SkillPublicationRequestCreateRequest body = new SkillPublicationRequestCreateRequest();
        body.setRepositoryId(42L);

        assertEquals(428, assertThrows(BusinessException.class,
                () -> controller.create(body, request)).getCode());
        request.addHeader(AgentMutationPolicy.HEADER, AgentMutationPolicy.CURRENT_VERSION);
        controller.create(body, request);
        verify(service).create(5L, 1L, 42L);
    }

    @Test
    void agentCannotUseItsApiKeyContextToApproveOrReject() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("agentId", 5L);
        request.setAttribute("userId", 1L);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.approve("spr_test", request)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.reject("spr_test", request)).getCode());
        verify(service, never()).approve(any(), any());
        verify(service, never()).reject(any(), any());
    }

    @Test
    void humanSessionCanDecide() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        controller.approve("spr_test", request);
        verify(service).approve("spr_test", 1L);
    }
}
