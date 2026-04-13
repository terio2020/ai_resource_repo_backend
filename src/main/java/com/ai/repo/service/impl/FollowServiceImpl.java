package com.ai.repo.service.impl;

import com.ai.repo.entity.Agent;
import com.ai.repo.entity.Follow;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.FollowMapper;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private AgentService agentService;

    @Override
    @Transactional
    public boolean follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException("Cannot follow yourself");
        }

        if (isFollowing(followerId, followingId)) {
            return false;
        }

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        followMapper.insert(follow);

        agentService.incrementFollowingCount(followerId, 1);
        agentService.incrementFollowerCount(followingId, 1);

        return true;
    }

    @Override
    @Transactional
    public boolean unfollow(Long followerId, Long followingId) {
        if (!isFollowing(followerId, followingId)) {
            return false;
        }

        boolean result = followMapper.deleteByFollow(followerId, followingId) > 0;

        if (result) {
            agentService.incrementFollowingCount(followerId, -1);
            agentService.incrementFollowerCount(followingId, -1);
        }

        return result;
    }

    @Override
    public List<Follow> findFollowing(Long followerId) {
        return followMapper.selectByFollowerId(followerId);
    }

    @Override
    public List<Follow> findFollowers(Long followingId) {
        return followMapper.selectByFollowingId(followingId);
    }

    @Override
    public Long countFollowing(Long agentId) {
        return followMapper.countFollowing(agentId);
    }

    @Override
    public Long countFollowers(Long agentId) {
        return followMapper.countFollowers(agentId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.selectByFollow(followerId, followingId) != null;
    }
}