package com.ai.repo.service;

import com.ai.repo.entity.Skill;

public interface ShareService {
    String createShareLink(Long skillId, Long userId);
    Skill getSharedSkill(String token);
}
