package com.ai.repo.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ai.repo.dto.FileTreeEntry;
import com.ai.repo.entity.SkillPublicationRequest;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SkillPublicationRequestMapper;
import com.ai.repo.service.SkillRepositoryService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillPublicationRequestServiceImplTest {
    @TempDir Path tempDir;
    @Mock SkillPublicationRequestMapper requestMapper;
    @Mock SkillRepositoryService repositoryService;
    private SkillPublicationRequestServiceImpl service;
    private SkillRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        Path gitDir = tempDir.resolve("skill.git");
        try (Git ignored = Git.init().setBare(true).setDirectory(gitDir.toFile()).call()) {
            // Repository fixture with a stable master ref.
        }
        Path ref = gitDir.resolve("refs/heads/master");
        Files.createDirectories(ref.getParent());
        Files.writeString(ref, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n");

        repo = new SkillRepository();
        repo.setId(42L);
        repo.setUserId(1L);
        repo.setAgentId(5L);
        repo.setSkillName("safe-skill");
        repo.setVersion("1.0.0");
        repo.setRepoPath(gitDir.toString());
        repo.setIsPublic(false);
        service = new SkillPublicationRequestServiceImpl();
        ReflectionTestUtils.setField(service, "requestMapper", requestMapper);
        ReflectionTestUtils.setField(service, "repositoryService", repositoryService);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://logicomanet.com/");
    }

    @Test
    void createProducesOwnerScopedLinkWithoutPublishing() {
        when(repositoryService.findById(42L)).thenReturn(repo);
        when(repositoryService.getFileTree(42L)).thenReturn(
                List.of(FileTreeEntry.builder().path("SKILL.md").size(100).build()));

        var created = service.create(5L, 1L, 42L);

        assertTrue(created.requestId().matches("spr_[0-9a-f]{64}"));
        assertEquals("https://logicomanet.com/approve/skill-publication/" + created.requestId(),
                created.approvalUrl());
        verify(requestMapper).insert(argThat(request ->
                request.getAgentId().equals(5L)
                        && request.getUserId().equals(1L)
                        && request.getRepositoryId().equals(42L)
                        && request.getHeadCommit().equals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                        && !request.getTokenHash().contains(created.requestId())));
        verify(repositoryService, never()).setVisibility(any(), any(), anyBoolean());
    }

    @Test
    void onlyOwnerCanApproveAndApprovalPublishesOnce() {
        SkillPublicationRequest request = pendingRequest();
        when(requestMapper.selectByTokenHash(any())).thenReturn(request);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.approve(requestId(), 9L)).getCode());
        verify(repositoryService, never()).setVisibility(any(), any(), anyBoolean());

        when(repositoryService.findById(42L)).thenReturn(repo);
        when(requestMapper.decide(7L, 1L, "APPROVED")).thenReturn(1);
        service.approve(requestId(), 1L);
        verify(repositoryService).setVisibility(42L, 1L, true);
        verify(requestMapper).decide(7L, 1L, "APPROVED");
    }

    @Test
    void changedCommitCannotBeApproved() throws Exception {
        SkillPublicationRequest request = pendingRequest();
        when(requestMapper.selectByTokenHash(any())).thenReturn(request);
        when(repositoryService.findById(42L)).thenReturn(repo);
        Files.writeString(Path.of(repo.getRepoPath(), "refs/heads/master"),
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\n");

        assertEquals("CHANGED", service.details(requestId(), 1L).status());
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.approve(requestId(), 1L)).getCode());
        verify(repositoryService, never()).setVisibility(any(), any(), anyBoolean());
    }

    @Test
    void expiredOrRejectedRequestCannotPublish() {
        SkillPublicationRequest request = pendingRequest();
        request.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(requestMapper.selectByTokenHash(any())).thenReturn(request);
        assertEquals("EXPIRED", service.status(requestId(), 5L).status());
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.approve(requestId(), 1L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.status(requestId(), 8L)).getCode());
        verify(repositoryService, never()).setVisibility(any(), any(), anyBoolean());
    }

    @Test
    void metadataChangeInvalidatesReviewWithoutChangingCommit() {
        SkillPublicationRequest request = pendingRequest();
        when(requestMapper.selectByTokenHash(any())).thenReturn(request);
        when(repositoryService.findById(42L)).thenReturn(repo);
        repo.setDescription("Not reviewed by the owner");
        assertEquals("CHANGED", service.status(requestId(), 5L).status());
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.approve(requestId(), 1L)).getCode());
        verify(repositoryService, never()).setVisibility(any(), any(), anyBoolean());
    }

    private SkillPublicationRequest pendingRequest() {
        SkillPublicationRequest request = new SkillPublicationRequest();
        request.setId(7L);
        request.setTokenHash("hash");
        request.setUserId(1L);
        request.setAgentId(5L);
        request.setRepositoryId(42L);
        request.setHeadCommit("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        request.setMetadataHash(ReflectionTestUtils.invokeMethod(service, "metadataHash", repo));
        request.setStatus("PENDING");
        request.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return request;
    }

    private String requestId() {
        return "spr_" + "a".repeat(64);
    }
}
