package com.ai.repo.service.impl;

import com.ai.repo.entity.ShareLink;
import com.ai.repo.entity.Skill;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.ShareLinkMapper;
import com.ai.repo.mapper.SkillMapper;
import com.ai.repo.service.ShareService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ShareServiceImpl implements ShareService {

    @Resource
    private ShareLinkMapper shareLinkMapper;

    @Resource
    private SkillMapper skillMapper;

    @Override
    public String createShareLink(Long skillId, Long userId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new BusinessException(404, "Skill not found");
        }
        if (!Boolean.TRUE.equals(skill.getIsPublic())) {
            throw new BusinessException(400, "Only public skills can be shared");
        }

        ShareLink existing = shareLinkMapper.findBySkillAndCreator(skillId, userId);
        if (existing != null) {
            return existing.getShareToken();
        }

        ShareLink shareLink = new ShareLink();
        shareLink.setSkillId(skillId);
        shareLink.setShareToken(UUID.randomUUID().toString());
        shareLink.setCreatedBy(userId);
        shareLink.setViewCount(0);

        shareLinkMapper.insert(shareLink);
        return shareLink.getShareToken();
    }

    @Override
    public Skill getSharedSkill(String token) {
        ShareLink shareLink = shareLinkMapper.findByToken(token);
        if (shareLink == null) {
            throw new BusinessException(404, "Shared link not found or expired");
        }

        shareLinkMapper.incrementViewCount(shareLink.getId());

        Skill skill = skillMapper.selectById(shareLink.getSkillId());
        if (skill == null) {
            throw new BusinessException(404, "The shared skill is no longer available");
        }

        return skill;
    }
}
