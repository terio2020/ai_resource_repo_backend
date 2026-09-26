package com.ai.repo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.AgentMutationPolicy;
import com.ai.repo.service.MemoryPublicationRequestService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemoryPublicationRequestControllerTest {
    @Test void agentCanCreateAndPollButCannotReviewOrDecide() {
        var service = mock(MemoryPublicationRequestService.class);
        var controller = new MemoryPublicationRequestController();
        ReflectionTestUtils.setField(controller, "service", service);
        var request = new MockHttpServletRequest();
        request.setAttribute("agentId", 5L); request.setAttribute("userId", 1L);
        var body = new MemoryPublicationRequestController.CreateRequest(42L);
        assertEquals(428, assertThrows(BusinessException.class, () -> controller.create(body, request)).getCode());
        request.addHeader(AgentMutationPolicy.HEADER, AgentMutationPolicy.CURRENT_VERSION);
        controller.create(body, request); controller.status("mpr_test", request);
        verify(service).create(5L, 1L, 42L); verify(service).status("mpr_test", 5L);
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.details("mpr_test", request)).getCode());
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.approve("mpr_test", request)).getCode());
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.reject("mpr_test", request)).getCode());
        verify(service, never()).approve(any(), any());
    }
}
