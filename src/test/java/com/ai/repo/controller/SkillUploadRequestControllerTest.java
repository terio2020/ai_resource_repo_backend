package com.ai.repo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.ai.repo.dto.SkillUploadRequestCreateRequest;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.AgentMutationPolicy;
import com.ai.repo.service.SkillPublicationRequestService;
import com.ai.repo.service.SkillUploadRequestService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SkillUploadRequestControllerTest {
    @Mock SkillUploadRequestService uploadService;
    @Mock SkillPublicationRequestService publicationService;
    private SkillUploadRequestController controller;

    @BeforeEach
    void setUp() {
        controller = new SkillUploadRequestController();
        ReflectionTestUtils.setField(controller, "uploadService", uploadService);
        ReflectionTestUtils.setField(controller, "publicationService", publicationService);
    }

    @Test
    void agentNeedsCurrentPolicyToRequestPrivateUpload() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("agentId", 5L);
        request.setAttribute("userId", 1L);
        SkillUploadRequestCreateRequest body = new SkillUploadRequestCreateRequest();
        assertEquals(428, assertThrows(BusinessException.class,
                () -> controller.create(body, request)).getCode());
        request.addHeader(AgentMutationPolicy.HEADER, AgentMutationPolicy.CURRENT_VERSION);
        controller.create(body, request);
        verify(uploadService).create(5L, 1L, body);
    }

    @Test
    void agentCannotApproveRejectOrOpenPublicLink() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("agentId", 5L);
        request.setAttribute("userId", 1L);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.approve("sur_test", request)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.reject("sur_test", request)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.publicationLink("sur_test", request)).getCode());
        verify(uploadService, never()).approve(any(), any());
        verify(uploadService, never()).reject(any(), any());
        verify(publicationService, never()).create(any(), any(), any());
    }
}
