package com.ai.repo.service.impl;

import com.ai.repo.dto.PublicationGrantCreateRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.PublicationGrant;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.PublicationGrantMapper;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.SkillRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicationGrantServiceImplTest {
    @Mock private PublicationGrantMapper mapper;
    @Mock private AgentService agentService;
    @Mock private SkillRepositoryService repositoryService;
    private PublicationGrantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicationGrantServiceImpl();
        ReflectionTestUtils.setField(service, "publicationGrantMapper", mapper);
        ReflectionTestUtils.setField(service, "agentService", agentService);
        ReflectionTestUtils.setField(service, "skillRepositoryService", repositoryService);
    }

    @Test
    void issuesOpaqueHashedSingleResourceGrant() {
        Agent agent = new Agent();
        agent.setId(5L);
        agent.setUserId(1L);
        when(agentService.findById(5L)).thenReturn(agent);
        SkillRepository repo = new SkillRepository();
        repo.setId(42L);
        repo.setAgentId(5L);
        repo.setUserId(1L);
        when(repositoryService.findById(42L)).thenReturn(repo);

        PublicationGrantCreateRequest request = new PublicationGrantCreateRequest();
        request.setAgentId(5L);
        request.setResourceType("skill_repository");
        request.setResourceId(42L);
        var response = service.issue(1L, request);

        assertTrue(response.getToken().startsWith("pgr_"));
        assertNotNull(response.getExpiresAt());
        ArgumentCaptor<PublicationGrant> captor = ArgumentCaptor.forClass(PublicationGrant.class);
        verify(mapper).insert(captor.capture());
        assertEquals(64, captor.getValue().getTokenHash().length());
        assertTrue(!captor.getValue().getTokenHash().contains(response.getToken()));
    }

    @Test
    void rejectsGrantForAnotherUsersAgent() {
        Agent agent = new Agent();
        agent.setId(5L);
        agent.setUserId(9L);
        when(agentService.findById(5L)).thenReturn(agent);
        PublicationGrantCreateRequest request = new PublicationGrantCreateRequest();
        request.setAgentId(5L);
        request.setResourceType("MEMORY");
        request.setResourceKey("profile");
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.issue(1L, request)).getCode());
    }

    @Test
    void consumeFailsClosedUnlessExactlyOneRowMatches() {
        when(mapper.consume(any(), any(), any(), any(), any(), any())).thenReturn(0);
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.consume("pgr_test", 1L, 5L, "MEMORY", null, "memory-key"));
        assertEquals(403, error.getCode());
    }
}
