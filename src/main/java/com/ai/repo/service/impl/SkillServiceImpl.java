package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.entity.Skill;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SkillMapper;
import com.ai.repo.service.SkillService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SkillServiceImpl implements SkillService {

    @Resource
    private SkillMapper skillMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PUBLIC_SKILLS_CACHE_KEY = "cache:skills:public";
    private static final long PUBLIC_SKILLS_CACHE_TTL = 60; // seconds

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
        // Redis cache for public skills (high-frequency anonymous endpoint)
        if (Boolean.TRUE.equals(isPublic)) {
            @SuppressWarnings("unchecked")
            List<Skill> cached = (List<Skill>) redisTemplate.opsForValue().get(PUBLIC_SKILLS_CACHE_KEY);
            if (cached != null) {
                return cached;
            }
            List<Skill> skills = skillMapper.selectByPublic(isPublic);
            redisTemplate.opsForValue().set(PUBLIC_SKILLS_CACHE_KEY, skills, PUBLIC_SKILLS_CACHE_TTL, TimeUnit.SECONDS);
            return skills;
        }
        return skillMapper.selectByPublic(isPublic);
    }

    @Override
    public List<Skill> searchByKeyword(String keyword) {
        return skillMapper.searchByKeyword(keyword);
    }

    @Override
    public boolean incrementDownloadCount(Long id) {
        int rows = skillMapper.incrementDownloadCount(id);
        if (rows == 0) {
            throw new BusinessException("Skill not found");
        }
        return true;
    }

    @Override
    public boolean incrementLikeCount(Long id) {
        int rows = skillMapper.incrementLikeCount(id);
        if (rows == 0) {
            throw new BusinessException("Skill not found");
        }
        return true;
    }

    @Override
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("IDs cannot be null or empty");
        }
        return skillMapper.batchDelete(ids);
    }

    @Override
    public PageResult<Skill> findAllPaginated(int page, int pageSize) {
        long total = skillMapper.countAll();
        long offset = (long) (page - 1) * pageSize;
        List<Skill> records = skillMapper.selectAllPaginated(offset, pageSize);
        return new PageResult<>(records, total, (long) page, (long) pageSize);
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
