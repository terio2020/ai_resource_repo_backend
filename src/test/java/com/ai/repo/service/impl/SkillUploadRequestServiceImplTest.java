package com.ai.repo.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ai.repo.dto.FileTreeEntry;
import com.ai.repo.dto.SkillUploadRequestCreateRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.entity.SkillUploadRequest;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SkillUploadRequestMapper;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.SkillRepositoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillUploadRequestServiceImplTest {
    @TempDir Path tempDir;
    @Mock SkillUploadRequestMapper mapper;
    @Mock SkillRepositoryService repositoryService;
    @Mock AgentService agentService;
    private SkillUploadRequestServiceImpl service;
    private SkillUploadRequestCreateRequest body;

    @BeforeEach
    void setUp() {
        service = new SkillUploadRequestServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "repositoryService", repositoryService);
        ReflectionTestUtils.setField(service, "agentService", agentService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "frontendUrl", "https://logicomanet.com/");
        body = new SkillUploadRequestCreateRequest();
        body.setSkillName("safe-skill");
        body.setVersion("1.0.0");
        body.setDescription("Safe reusable workflow");
        body.setTags("safe,workflow,utility");
        body.setCategory("utility");
        body.setType("skill");
        body.setExpectedCommit("a".repeat(40));
        body.setFiles(List.of(new FileTreeEntry("SKILL.md", 10)));
    }

    @Test
    void createsOwnerScopedLinkWithoutUploadingOrPublishing() {
        Agent agent = new Agent();
        agent.setId(5L);
        agent.setUserId(1L);
        when(agentService.findById(5L)).thenReturn(agent);
        when(repositoryService.findByAgentId(5L)).thenReturn(List.of());

        var created = service.create(5L, 1L, body);

        assertTrue(created.requestId().matches("sur_[0-9a-f]{64}"));
        assertEquals("https://logicomanet.com/approve/skill-upload/" + created.requestId(),
                created.approvalUrl());
        verify(mapper).insert(argThat(request -> request.getUserId().equals(1L)
                && request.getAgentId().equals(5L) && request.getRepositoryId() == null
                && request.getManifestJson().contains("SKILL.md")
                && !request.getTokenHash().contains(created.requestId())));
        verify(repositoryService, never()).create(any());
    }

    @Test
    void ownerApprovalCoversCreateAndFirstPushStages() throws Exception {
        SkillUploadRequest request = pendingRequest();
        when(mapper.selectByTokenHash(any())).thenReturn(request);
        when(mapper.decide(7L, 1L, "APPROVED")).thenReturn(1);
        when(mapper.claimCreate(7L, 1L, 5L)).thenReturn(1);
        when(mapper.finishCreate(7L, 42L)).thenReturn(1);
        when(mapper.consumePush(7L, 42L, 5L)).thenReturn(1);

        service.approve(requestId(), 1L);
        service.claimCreate(requestId(), 1L, 5L, body);
        service.finishCreate(requestId(), 42L);
        request.setStatus("CREATED");
        request.setRepositoryId(42L);

        Path repoDir = tempDir.resolve("skill");
        Files.createDirectories(repoDir);
        Files.writeString(repoDir.resolve("SKILL.md"), "0123456789");
        try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
            git.add().addFilepattern("SKILL.md").call();
            ObjectId commit = git.commit().setMessage("First upload")
                    .setAuthor("Agent", "agent@example.invalid").call().getId();
            request.setExpectedCommit(commit.name());
            service.verifyFirstPush(requestId(), 1L, 5L, 42L, git.getRepository(),
                    ObjectId.zeroId(), commit);
            service.completeFirstPush(requestId(), 5L, 42L);

            assertEquals(403, assertThrows(BusinessException.class,
                    () -> service.verifyFirstPush(requestId(), 1L, 5L, 42L,
                            git.getRepository(), commit, commit)).getCode());
        }
    }

    @Test
    void rejectsAnotherOwnerAndChangedManifest() throws Exception {
        SkillUploadRequest request = pendingRequest();
        request.setStatus("CREATED");
        request.setRepositoryId(42L);
        when(mapper.selectByTokenHash(any())).thenReturn(request);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.details(requestId(), 9L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.status(requestId(), 8L)).getCode());

        Path repoDir = tempDir.resolve("changed");
        Files.createDirectories(repoDir);
        Files.writeString(repoDir.resolve("SKILL.md"), "different contents");
        try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
            git.add().addFilepattern("SKILL.md").call();
            ObjectId commit = git.commit().setMessage("Changed")
                    .setAuthor("Agent", "agent@example.invalid").call().getId();
            request.setExpectedCommit(commit.name());
            assertEquals(403, assertThrows(BusinessException.class,
                    () -> service.verifyFirstPush(requestId(), 1L, 5L, 42L,
                            git.getRepository(), ObjectId.zeroId(), commit)).getCode());
        }
        verify(mapper, never()).consumePush(any(), any(), any());
    }

    @Test
    void expiredApprovalCannotCreateOrPush() {
        SkillUploadRequest request = pendingRequest();
        request.setStatus("APPROVED");
        request.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(mapper.selectByTokenHash(any())).thenReturn(request);
        assertEquals("EXPIRED", service.status(requestId(), 5L).status());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.claimCreate(requestId(), 1L, 5L, body)).getCode());
    }

    @Test
    void emptyOwnedPrivateRepositoryCanRecoverWithNewApproval() throws Exception {
        Path bareDir = tempDir.resolve("empty.git");
        try (Git ignored = Git.init().setBare(true).setDirectory(bareDir.toFile()).call()) {
            // The failed first push left an empty bare repository.
        }
        SkillRepository repo = new SkillRepository();
        repo.setId(42L);
        repo.setAgentId(5L);
        repo.setUserId(1L);
        repo.setSkillName(body.getSkillName());
        repo.setVersion(body.getVersion());
        repo.setDescription(body.getDescription());
        repo.setTags(body.getTags());
        repo.setCategory(body.getCategory());
        repo.setType(body.getType());
        repo.setIsPublic(false);
        repo.setRepoPath(bareDir.toString());
        body.setRepositoryId(42L);
        Agent agent = new Agent();
        agent.setId(5L);
        agent.setUserId(1L);
        when(agentService.findById(5L)).thenReturn(agent);
        when(repositoryService.findById(42L)).thenReturn(repo);
        service.create(5L, 1L, body);

        SkillUploadRequest request = pendingRequest();
        request.setRepositoryId(42L);
        when(mapper.selectByTokenHash(any())).thenReturn(request);
        when(mapper.decide(7L, 1L, "CREATED")).thenReturn(1);
        service.approve(requestId(), 1L);
        verify(mapper).decide(7L, 1L, "CREATED");
    }

    private SkillUploadRequest pendingRequest() {
        SkillUploadRequest request = new SkillUploadRequest();
        request.setId(7L);
        request.setUserId(1L);
        request.setAgentId(5L);
        request.setSkillName(body.getSkillName());
        request.setVersion(body.getVersion());
        request.setDescription(body.getDescription());
        request.setTags(body.getTags());
        request.setCategory(body.getCategory());
        request.setType(body.getType());
        request.setExpectedCommit(body.getExpectedCommit());
        request.setManifestJson("[{\"path\":\"SKILL.md\",\"size\":10}]");
        request.setStatus("PENDING");
        request.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return request;
    }

    private String requestId() {
        return "sur_" + "a".repeat(64);
    }
}
