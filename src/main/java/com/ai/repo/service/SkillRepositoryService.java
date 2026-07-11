package com.ai.repo.service;

import com.ai.repo.dto.FileTreeEntry;
import com.ai.repo.entity.SkillRepository;

import java.util.List;

public interface SkillRepositoryService {

    SkillRepository findById(Long id);

    SkillRepository findByUid(String uid);

    List<SkillRepository> findByAgentId(Long agentId);

    SkillRepository create(SkillRepository skillRepository);

    void delete(Long id);

    SkillRepository updateMetadata(SkillRepository repo);

    SkillRepository forkRepository(Long currentAgentId, Long currentUserId, Long sourceRepoId);

    void setVisibility(Long repoId, Long requestUserId, boolean isPublic);

    void incrementDownloadCount(Long repoId);

    void incrementLikeCount(Long repoId);

    List<SkillRepository> findPublicRepos();

    List<SkillRepository> findPublicReposByAgentId(Long agentId);

    List<SkillRepository> findByCategory(String category);

    List<SkillRepository> findByType(String type);

    List<SkillRepository> searchByKeyword(String keyword);

    SkillRepository findByRepoPath(String repoPath);

    SkillRepository findByShareId(String shareId);

    List<FileTreeEntry> getFileTree(Long repoId);

    String getFileContent(Long repoId, String filePath);

    List<SkillRepository> findForksByParentId(Long parentId);
}
