package com.ai.repo.service;

import com.ai.repo.entity.Skill;

import java.util.List;

public interface SkillService {
    Skill create(Skill skill);
    Skill update(Skill skill);
    boolean delete(Long id);
    Skill findById(Long id);
    List<Skill> findAll();
    List<Skill> findByUserId(Long userId);
    List<Skill> findByAgentId(Long agentId);
    List<Skill> findByCategory(String category);
    List<Skill> findByPublic(Boolean isPublic);
    List<Skill> searchByKeyword(String keyword);
    boolean incrementDownloadCount(Long id);
    boolean incrementLikeCount(Long id);
    int batchDelete(List<Long> ids);
}
