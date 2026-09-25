package com.ai.repo.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import jakarta.annotation.Resource;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ai.repo.dto.FileTreeEntry;
import com.ai.repo.entity.SkillPublicationRequest;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SkillPublicationRequestMapper;
import com.ai.repo.service.SkillPublicationRequestService;
import com.ai.repo.service.SkillRepositoryService;

@Service
public class SkillPublicationRequestServiceImpl implements SkillPublicationRequestService {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Resource
    private SkillPublicationRequestMapper requestMapper;

    @Resource
    private SkillRepositoryService repositoryService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public Created create(Long agentId, Long userId, Long repositoryId) {
        SkillRepository repo = requireOwnedRepository(repositoryId, userId, agentId);
        requirePrivateAndVisible(repo);
        List<FileTreeEntry> files = repositoryService.getFileTree(repositoryId);
        if (files.stream().noneMatch(file -> "SKILL.md".equals(file.getPath()))) {
            throw new BusinessException(409, "Upload SKILL.md before requesting publication");
        }

        String headCommit = currentHead(repo);
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String requestId = "spr_" + HexFormat.of().formatHex(random);
        SkillPublicationRequest request = new SkillPublicationRequest();
        request.setTokenHash(hash(requestId));
        request.setUserId(userId);
        request.setAgentId(agentId);
        request.setRepositoryId(repositoryId);
        request.setHeadCommit(headCommit);
        request.setStatus("PENDING");
        request.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        requestMapper.insert(request);
        String approvalUrl = frontendUrl.replaceAll("/+$", "")
                + "/approve/skill-publication/" + requestId;
        return new Created(requestId, approvalUrl, repositoryId, request.getExpiresAt());
    }

    @Override
    public Details details(String requestId, Long userId) {
        SkillPublicationRequest request = requireRequest(requestId);
        if (!request.getUserId().equals(userId)) {
            throw new BusinessException(403, "This publication request belongs to another user");
        }
        SkillRepository repo = requireOwnedRepository(
                request.getRepositoryId(), userId, request.getAgentId());
        String status = effectiveStatus(request);
        if ("PENDING".equals(status) && (Boolean.TRUE.equals(repo.getIsPublic())
                || "BANNED".equals(repo.getStatus())
                || !request.getHeadCommit().equals(currentHead(repo)))) {
            status = "CHANGED";
        }
        return new Details(status, repo.getId(), repo.getAgentId(), repo.getSkillName(),
                repo.getVersion(), repo.getDescription(), request.getHeadCommit(),
                repositoryService.getFileTree(repo.getId()), request.getExpiresAt());
    }

    @Override
    public Status status(String requestId, Long agentId) {
        SkillPublicationRequest request = requireRequest(requestId);
        if (!request.getAgentId().equals(agentId)) {
            throw new BusinessException(403, "This publication request belongs to another Agent");
        }
        String status = effectiveStatus(request);
        if ("PENDING".equals(status)) {
            SkillRepository repo = requireOwnedRepository(
                    request.getRepositoryId(), request.getUserId(), agentId);
            if (Boolean.TRUE.equals(repo.getIsPublic()) || "BANNED".equals(repo.getStatus())
                    || !request.getHeadCommit().equals(currentHead(repo))) {
                status = "CHANGED";
            }
        }
        return new Status(status, request.getRepositoryId(), request.getExpiresAt());
    }

    @Override
    @Transactional
    public void approve(String requestId, Long userId) {
        SkillPublicationRequest request = requireRequest(requestId);
        if (!request.getUserId().equals(userId)) {
            throw new BusinessException(403, "This publication request belongs to another user");
        }
        if ("APPROVED".equals(request.getStatus())) {
            SkillRepository published = requireOwnedRepository(
                    request.getRepositoryId(), userId, request.getAgentId());
            if (Boolean.TRUE.equals(published.getIsPublic())) {
                return;
            }
        }
        if (!"PENDING".equals(effectiveStatus(request))) {
            throw new BusinessException(409, "Publication request is no longer pending");
        }
        SkillRepository repo = requireOwnedRepository(
                request.getRepositoryId(), userId, request.getAgentId());
        requirePrivateAndVisible(repo);
        if (!request.getHeadCommit().equals(currentHead(repo))) {
            throw new BusinessException(409, "Skill contents changed; request a new approval link");
        }
        if (requestMapper.decide(request.getId(), userId, "APPROVED") != 1) {
            throw new BusinessException(409, "Publication request is no longer pending");
        }
        repositoryService.setVisibility(repo.getId(), userId, true);
    }

    @Override
    public void reject(String requestId, Long userId) {
        SkillPublicationRequest request = requireRequest(requestId);
        if (!request.getUserId().equals(userId)) {
            throw new BusinessException(403, "This publication request belongs to another user");
        }
        if (requestMapper.decide(request.getId(), userId, "REJECTED") != 1) {
            throw new BusinessException(409, "Publication request is no longer pending");
        }
    }

    private SkillPublicationRequest requireRequest(String requestId) {
        if (requestId == null || !requestId.matches("spr_[0-9a-f]{64}")) {
            throw new BusinessException(404, "Publication request not found");
        }
        SkillPublicationRequest request = requestMapper.selectByTokenHash(hash(requestId));
        if (request == null) {
            throw new BusinessException(404, "Publication request not found");
        }
        return request;
    }

    private SkillRepository requireOwnedRepository(Long repositoryId, Long userId, Long agentId) {
        SkillRepository repo = repositoryService.findById(repositoryId);
        if (!userId.equals(repo.getUserId()) || !agentId.equals(repo.getAgentId())) {
            throw new BusinessException(403, "Repository is not owned by this Agent and user");
        }
        return repo;
    }

    private void requirePrivateAndVisible(SkillRepository repo) {
        if (Boolean.TRUE.equals(repo.getIsPublic()) || "BANNED".equals(repo.getStatus())) {
            throw new BusinessException(409, "Only a private, visible Skill can be published");
        }
    }

    private String effectiveStatus(SkillPublicationRequest request) {
        if ("PENDING".equals(request.getStatus())
                && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            return "EXPIRED";
        }
        return request.getStatus();
    }

    private String currentHead(SkillRepository repo) {
        try (Repository git = new FileRepositoryBuilder()
                .setGitDir(java.nio.file.Path.of(repo.getRepoPath()).toFile())
                .setMustExist(true).build()) {
            ObjectId head = git.resolve("refs/heads/master");
            if (head == null) {
                throw new BusinessException(409, "Upload the Skill before requesting publication");
            }
            return head.name();
        } catch (IOException e) {
            throw new BusinessException(409, "Skill Git repository is unavailable");
        }
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
