package com.ai.repo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import com.ai.repo.dto.FileTreeEntry;
import com.ai.repo.dto.SkillRepositoryCreateRequest;
import com.ai.repo.dto.SkillUploadRequestCreateRequest;

public interface SkillUploadRequestService {
    record Created(String requestId, String approvalUrl, LocalDateTime expiresAt) {}

    record Details(String status, Long agentId, Long repositoryId, String skillName,
                   String version, String description, String tags, String category,
                   String type, String expectedCommit, List<FileTreeEntry> files,
                   LocalDateTime expiresAt) {}

    record Status(String status, Long repositoryId, LocalDateTime expiresAt) {}

    Created create(Long agentId, Long userId, SkillUploadRequestCreateRequest body);

    Details details(String requestId, Long userId);

    Status status(String requestId, Long agentId);

    void approve(String requestId, Long userId);

    void reject(String requestId, Long userId);

    void claimCreate(String requestId, Long userId, Long agentId,
                     SkillRepositoryCreateRequest metadata);

    void finishCreate(String requestId, Long repositoryId);

    void verifyFirstPush(String requestId, Long userId, Long agentId, Long repositoryId,
                         Repository git, ObjectId oldId, ObjectId newId);

    void completeFirstPush(String requestId, Long agentId, Long repositoryId);

    Long publicationReadyRepository(String requestId, Long userId);
}
