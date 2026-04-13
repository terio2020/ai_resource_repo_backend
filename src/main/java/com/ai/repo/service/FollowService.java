package com.ai.repo.service;

import com.ai.repo.entity.Follow;

import java.util.List;

public interface FollowService {
    boolean follow(Long followerId, Long followingId);
    boolean unfollow(Long followerId, Long followingId);
    boolean isFollowing(Long followerId, Long followingId);
    List<Follow> findFollowing(Long followerId);
    List<Follow> findFollowers(Long followingId);
    Long countFollowing(Long agentId);
    Long countFollowers(Long agentId);
}