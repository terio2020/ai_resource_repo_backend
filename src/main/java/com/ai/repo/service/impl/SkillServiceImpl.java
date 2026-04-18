package com.ai.repo.service.impl;

import com.ai.repo.entity.Skill;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SkillMapper;
import com.ai.repo.service.SkillService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillServiceImpl implements SkillService {

    @Resource
    private SkillMapper skillMapper;

    @Override
    public Skill create(Skill skill) {
        skillMapper.insert(skill);
        return skill;
    }

    @Override
    public Skill update(Skill skill) {
        if (skillMapper.selectById(skill.getId()) == null) {
            throw new BusinessException("Skill not found");
        }
        skillMapper.update(skill);
        return skill;
    }

    @Override
    public boolean delete(Long id) {
        if (skillMapper.selectById(id) == null) {
            throw new BusinessException("Skill not found");
        }
        return skillMapper.deleteById(id) > 0;
    }

    @Override
    public Skill findById(Long id) {
        return skillMapper.selectById(id);
    }

    @Override
    public List<Skill> findAll() {
        return skillMapper.selectAll();
    }

    @Override
    public List<Skill> findByUserId(Long userId) {
        return skillMapper.selectByUserId(userId);
    }

    @Override
    public List<Skill> findByAgentId(Long agentId) {
        return skillMapper.selectByAgentId(agentId);
    }

    @Override
    public List<Skill> findByCategory(String category) {
        return skillMapper.selectByCategory(category);
    }

    @Override
    public List<Skill> findByPublic(Boolean isPublic) {
        return skillMapper.selectByPublic(isPublic);
    }

    @Override
    public List<Skill> searchByKeyword(String keyword) {
        return skillMapper.searchByKeyword(keyword);
    }

    @Override
    public boolean incrementDownloadCount(Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException("Skill not found");
        }
        skill.setDownloadCount(skill.getDownloadCount() + 1);
        return skillMapper.update(skill) > 0;
    }

    @Override
    public boolean incrementLikeCount(Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException("Skill not found");
        }
        skill.setLikeCount(skill.getLikeCount() + 1);
        return skillMapper.update(skill) > 0;
    }

    @Override
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("IDs cannot be null or empty");
        }
        return skillMapper.batchDelete(ids);
    }

    @Override
    public Skill upsert(Skill skill) {
        Skill existingSkill = skillMapper.selectByUserIdAndAgentIdAndName(skill.getUserId(), skill.getAgentId(), skill.getName());

        if (existingSkill != null) {
            skill.setId(existingSkill.getId());
            skillMapper.updateByCompositeKey(skill);
            return skill;
        } else {
            skillMapper.insert(skill);
            return skill;
        }
    }
}
