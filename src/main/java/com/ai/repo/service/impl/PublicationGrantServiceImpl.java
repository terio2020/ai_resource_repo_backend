package com.ai.repo.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ai.repo.dto.PublicationGrantCreateRequest;
import com.ai.repo.dto.PublicationGrantResponse;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.PublicationGrant;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.PublicationGrantMapper;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.PublicationGrantService;
import com.ai.repo.service.SkillRepositoryService;

@Service
public class PublicationGrantServiceImpl implements PublicationGrantService {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Resource
    private PublicationGrantMapper publicationGrantMapper;
    @Resource
    private AgentService agentService;
    @Resource
    private SkillRepositoryService skillRepositoryService;

    @Override
    public PublicationGrantResponse issue(Long userId, PublicationGrantCreateRequest request) {
        Agent agent = agentService.findById(request.getAgentId());
        if (agent == null || !userId.equals(agent.getUserId())) {
            throw new BusinessException(403, "Agent does not belong to the authenticated user");
        }

        String resourceType = normalizeType(request.getResourceType());
        validateScope(userId, request, resourceType);

        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = "pgr_" + HexFormat.of().formatHex(random);
        int ttl = request.getExpiresInSeconds() == null ? 300 : request.getExpiresInSeconds();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttl);

        PublicationGrant grant = new PublicationGrant();
        grant.setTokenHash(hash(token));
        grant.setUserId(userId);
        grant.setAgentId(request.getAgentId());
        grant.setResourceType(resourceType);
        grant.setResourceId(request.getResourceId());
        grant.setResourceKey(blankToNull(request.getResourceKey()));
        grant.setExpiresAt(expiresAt);
        publicationGrantMapper.insert(grant);
        return new PublicationGrantResponse(token, expiresAt);
    }

    @Override
    @Transactional
    public void consume(String token, Long userId, Long agentId, String resourceType,
                        Long resourceId, String resourceKey) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(403, "A human-issued publication grant is required");
        }
        int consumed = publicationGrantMapper.consume(hash(token), userId, agentId,
                normalizeType(resourceType), resourceId, blankToNull(resourceKey));
        if (consumed != 1) {
            throw new BusinessException(403, "Publication grant is invalid, expired, consumed, or out of scope");
        }
    }

    private void validateScope(Long userId, PublicationGrantCreateRequest request, String resourceType) {
        boolean hasId = request.getResourceId() != null;
        boolean hasKey = blankToNull(request.getResourceKey()) != null;
        if ("MEMORY".equals(resourceType)) {
            if (hasId == hasKey) {
                throw new BusinessException(400, "MEMORY grants require exactly one resourceId or resourceKey");
            }
            return;
        }
        if (!hasId || hasKey) {
            throw new BusinessException(400, "SKILL_REPOSITORY grants require resourceId only");
        }
        SkillRepository repo = skillRepositoryService.findById(request.getResourceId());
        if (repo == null || !userId.equals(repo.getUserId()) || !request.getAgentId().equals(repo.getAgentId())) {
            throw new BusinessException(403, "Repository is not owned by the selected Agent");
        }
    }

    private String normalizeType(String value) {
        String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
        if (!"MEMORY".equals(normalized) && !"SKILL_REPOSITORY".equals(normalized)) {
            throw new BusinessException(400, "resourceType must be MEMORY or SKILL_REPOSITORY");
        }
        return normalized;
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
