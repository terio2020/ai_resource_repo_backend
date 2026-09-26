package com.ai.repo.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.Resource;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ai.repo.dto.FileTreeEntry;
import com.ai.repo.dto.SkillRepositoryCreateRequest;
import com.ai.repo.dto.SkillUploadRequestCreateRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.entity.SkillUploadRequest;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SkillUploadRequestMapper;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.SkillRepositoryService;
import com.ai.repo.service.SkillUploadRequestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SkillUploadRequestServiceImpl implements SkillUploadRequestService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 20L * 1024 * 1024;

    @Resource private SkillUploadRequestMapper mapper;
    @Resource private SkillRepositoryService repositoryService;
    @Resource private AgentService agentService;
    @Resource private ObjectMapper objectMapper;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public Created create(Long agentId, Long userId, SkillUploadRequestCreateRequest body) {
        Agent agent = agentService.findById(agentId);
        if (agent == null || !userId.equals(agent.getUserId())) {
            throw new BusinessException(403, "Agent does not belong to this user");
        }
        validateManifest(body.getFiles());
        if (body.getExpectedCommit() == null
                || !body.getExpectedCommit().matches("[0-9a-f]{40}")) {
            throw new BusinessException(400, "Expected Git commit is required");
        }
        if (body.getRepositoryId() == null) {
            boolean exists = repositoryService.findByAgentId(agentId).stream()
                    .anyMatch(repo -> body.getSkillName().equals(repo.getSkillName()));
            if (exists) {
                throw new BusinessException(409, "Skill name already exists; use its repository ID to resume");
            }
        } else {
            SkillRepository repo = repositoryService.findById(body.getRepositoryId());
            if (!userId.equals(repo.getUserId()) || !agentId.equals(repo.getAgentId())
                    || Boolean.TRUE.equals(repo.getIsPublic()) || "BANNED".equals(repo.getStatus())
                    || !sameMetadata(repo, body) || hasMasterCommit(repo)) {
                throw new BusinessException(409, "Only an empty owned private Skill can resume upload");
            }
        }

        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String requestId = "sur_" + HexFormat.of().formatHex(random);
        SkillUploadRequest request = new SkillUploadRequest();
        request.setTokenHash(hash(requestId));
        request.setUserId(userId);
        request.setAgentId(agentId);
        request.setRepositoryId(body.getRepositoryId());
        request.setSkillName(body.getSkillName());
        request.setVersion(body.getVersion());
        request.setDescription(body.getDescription());
        request.setTags(body.getTags());
        request.setCategory(body.getCategory());
        request.setType(body.getType());
        request.setExpectedCommit(body.getExpectedCommit());
        request.setManifestJson(writeManifest(body.getFiles()));
        request.setStatus("PENDING");
        request.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        mapper.insert(request);
        String approvalUrl = frontendUrl.replaceAll("/+$", "")
                + "/approve/skill-upload/" + requestId;
        return new Created(requestId, approvalUrl, request.getExpiresAt());
    }

    @Override
    public Details details(String requestId, Long userId) {
        SkillUploadRequest request = requireRequest(requestId);
        requireOwner(request, userId);
        return new Details(effectiveStatus(request), request.getAgentId(),
                request.getRepositoryId(), request.getSkillName(), request.getVersion(),
                request.getDescription(), request.getTags(), request.getCategory(),
                request.getType(), request.getExpectedCommit(), readManifest(request),
                request.getExpiresAt());
    }

    @Override
    public Status status(String requestId, Long agentId) {
        SkillUploadRequest request = requireRequest(requestId);
        if (!request.getAgentId().equals(agentId)) {
            throw new BusinessException(403, "Upload request belongs to another Agent");
        }
        return new Status(effectiveStatus(request), request.getRepositoryId(), request.getExpiresAt());
    }

    @Override
    public void approve(String requestId, Long userId) {
        SkillUploadRequest request = requireRequest(requestId);
        requireOwner(request, userId);
        if (request.getRepositoryId() != null) {
            SkillRepository repo = repositoryService.findById(request.getRepositoryId());
            if (!userId.equals(repo.getUserId()) || !request.getAgentId().equals(repo.getAgentId())
                    || Boolean.TRUE.equals(repo.getIsPublic()) || "BANNED".equals(repo.getStatus())
                    || !sameMetadata(repo, request) || hasMasterCommit(repo)) {
                throw new BusinessException(409, "Empty private Skill is no longer eligible for upload");
            }
        }
        if (mapper.decide(request.getId(), userId,
                request.getRepositoryId() == null ? "APPROVED" : "CREATED") != 1) {
            throw new BusinessException(409, "Upload request is no longer pending");
        }
    }

    @Override
    public void reject(String requestId, Long userId) {
        SkillUploadRequest request = requireRequest(requestId);
        requireOwner(request, userId);
        if (mapper.decide(request.getId(), userId, "REJECTED") != 1) {
            throw new BusinessException(409, "Upload request is no longer pending");
        }
    }

    @Override
    @Transactional
    public void claimCreate(String requestId, Long userId, Long agentId,
                            SkillRepositoryCreateRequest metadata) {
        SkillUploadRequest request = requireRequest(requestId);
        requireAgent(request, userId, agentId);
        if (request.getRepositoryId() != null || !sameMetadata(request, metadata)
                || mapper.claimCreate(request.getId(), userId, agentId) != 1) {
            throw new BusinessException(403, "Approved private upload does not match this repository");
        }
    }

    @Override
    public void finishCreate(String requestId, Long repositoryId) {
        SkillUploadRequest request = requireRequest(requestId);
        if (mapper.finishCreate(request.getId(), repositoryId) != 1) {
            throw new BusinessException(409, "Upload request creation stage was not claimed");
        }
    }

    @Override
    public void verifyFirstPush(String requestId, Long userId, Long agentId, Long repositoryId,
                                Repository git, ObjectId oldId, ObjectId newId) {
        SkillUploadRequest request = requireRequest(requestId);
        requireAgent(request, userId, agentId);
        if (!"CREATED".equals(request.getStatus())
                || !request.getExpiresAt().isAfter(LocalDateTime.now())
                || !repositoryId.equals(request.getRepositoryId())
                || !ObjectId.zeroId().equals(oldId)
                || !request.getExpectedCommit().equals(newId.name())) {
            throw new BusinessException(403, "Private upload approval is expired or out of scope");
        }
        if (!readManifest(request).equals(commitManifest(git, newId))) {
            throw new BusinessException(403, "Git files differ from the approved manifest");
        }
    }

    @Override
    public void completeFirstPush(String requestId, Long agentId, Long repositoryId) {
        SkillUploadRequest request = requireRequest(requestId);
        if (!agentId.equals(request.getAgentId())
                || mapper.consumePush(request.getId(), repositoryId, agentId) != 1) {
            throw new BusinessException(409, "Upload request could not be completed");
        }
    }

    @Override
    public Long publicationReadyRepository(String requestId, Long userId) {
        SkillUploadRequest request = requireRequest(requestId);
        requireOwner(request, userId);
        if (!"UPLOADED".equals(effectiveStatus(request)) || request.getRepositoryId() == null) {
            throw new BusinessException(409, "Private upload must finish before public review");
        }
        return request.getRepositoryId();
    }

    private SkillUploadRequest requireRequest(String requestId) {
        if (requestId == null || !requestId.matches("sur_[0-9a-f]{64}")) {
            throw new BusinessException(404, "Upload request not found");
        }
        SkillUploadRequest request = mapper.selectByTokenHash(hash(requestId));
        if (request == null) {
            throw new BusinessException(404, "Upload request not found");
        }
        return request;
    }

    private void requireOwner(SkillUploadRequest request, Long userId) {
        if (!request.getUserId().equals(userId)) {
            throw new BusinessException(403, "Upload request belongs to another user");
        }
    }

    private void requireAgent(SkillUploadRequest request, Long userId, Long agentId) {
        if (!request.getUserId().equals(userId) || !request.getAgentId().equals(agentId)) {
            throw new BusinessException(403, "Upload request belongs to another Agent");
        }
    }

    private String effectiveStatus(SkillUploadRequest request) {
        if ("CREATED".equals(request.getStatus()) && request.getRepositoryId() != null
                && request.getExpiresAt().isAfter(LocalDateTime.now())) {
            SkillRepository repo = repositoryService.findById(request.getRepositoryId());
            if (request.getExpectedCommit().equals(masterCommit(repo))) {
                return "UPLOADED";
            }
        }
        if (("PENDING".equals(request.getStatus()) || "APPROVED".equals(request.getStatus())
                || "CREATED".equals(request.getStatus()))
                && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            return "EXPIRED";
        }
        return request.getStatus();
    }

    private boolean sameMetadata(SkillUploadRequest request, SkillRepositoryCreateRequest data) {
        return Objects.equals(request.getSkillName(), data.getSkillName())
                && Objects.equals(request.getVersion(), data.getVersion())
                && Objects.equals(request.getDescription(), data.getDescription())
                && Objects.equals(request.getTags(), data.getTags())
                && Objects.equals(request.getCategory(), data.getCategory())
                && Objects.equals(request.getType(), data.getType());
    }

    private boolean sameMetadata(SkillRepository repo, SkillRepositoryCreateRequest data) {
        return Objects.equals(repo.getSkillName(), data.getSkillName())
                && Objects.equals(repo.getVersion(), data.getVersion())
                && Objects.equals(repo.getDescription(), data.getDescription())
                && Objects.equals(repo.getTags(), data.getTags())
                && Objects.equals(repo.getCategory(), data.getCategory())
                && Objects.equals(repo.getType(), data.getType());
    }

    private boolean sameMetadata(SkillRepository repo, SkillUploadRequest request) {
        return Objects.equals(repo.getSkillName(), request.getSkillName())
                && Objects.equals(repo.getVersion(), request.getVersion())
                && Objects.equals(repo.getDescription(), request.getDescription())
                && Objects.equals(repo.getTags(), request.getTags())
                && Objects.equals(repo.getCategory(), request.getCategory())
                && Objects.equals(repo.getType(), request.getType());
    }

    private boolean hasMasterCommit(SkillRepository repo) {
        return masterCommit(repo) != null;
    }

    private String masterCommit(SkillRepository repo) {
        try (Repository git = new FileRepositoryBuilder()
                .setGitDir(java.nio.file.Path.of(repo.getRepoPath()).toFile())
                .setMustExist(true).build()) {
            ObjectId head = git.resolve("refs/heads/master");
            return head == null ? null : head.name();
        } catch (IOException e) {
            throw new BusinessException(409, "Private Skill Git repository is unavailable");
        }
    }

    private void validateManifest(List<FileTreeEntry> files) {
        if (files == null || files.isEmpty() || files.size() > 200) {
            throw new BusinessException(400, "Provide 1 to 200 staged files");
        }
        Set<String> paths = new HashSet<>();
        long total = 0;
        boolean hasSkill = false;
        for (FileTreeEntry file : files) {
            String path = file == null ? null : file.getPath();
            if (path == null || path.isBlank() || path.length() > 512
                    || path.startsWith("/") || path.endsWith("/") || path.contains("\\")
                    || path.contains("//") || path.contains("\0")
                    || List.of(path.split("/")).stream().anyMatch(part -> part.equals(".") || part.equals(".."))
                    || !paths.add(path) || file.getSize() < 0 || file.getSize() > MAX_FILE_SIZE) {
                throw new BusinessException(400, "Invalid or duplicate staged file path or size");
            }
            total += file.getSize();
            hasSkill |= "SKILL.md".equals(path) && file.getSize() > 0 && file.getSize() <= 1024 * 1024;
        }
        if (!hasSkill || total > MAX_TOTAL_SIZE) {
            throw new BusinessException(400, "Staged Skill must contain SKILL.md within size limits");
        }
    }

    private List<FileTreeEntry> commitManifest(Repository git, ObjectId commitId) {
        try (RevWalk revWalk = new RevWalk(git); TreeWalk treeWalk = new TreeWalk(git)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            List<FileTreeEntry> files = new ArrayList<>();
            while (treeWalk.next()) {
                ObjectLoader loader = git.open(treeWalk.getObjectId(0));
                files.add(new FileTreeEntry(treeWalk.getPathString(), loader.getSize()));
            }
            files.sort((a, b) -> a.getPath().compareTo(b.getPath()));
            return files;
        } catch (IOException e) {
            throw new BusinessException(409, "Cannot inspect the proposed Git commit");
        }
    }

    private String writeManifest(List<FileTreeEntry> files) {
        List<FileTreeEntry> sorted = new ArrayList<>(files);
        sorted.sort((a, b) -> a.getPath().compareTo(b.getPath()));
        try {
            return objectMapper.writeValueAsString(sorted);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize Skill manifest", e);
        }
    }

    private List<FileTreeEntry> readManifest(SkillUploadRequest request) {
        try {
            return objectMapper.readValue(request.getManifestJson(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored Skill manifest is invalid", e);
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
