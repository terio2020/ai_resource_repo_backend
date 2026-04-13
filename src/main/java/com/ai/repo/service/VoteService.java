package com.ai.repo.service;

import com.ai.repo.entity.Vote;

import java.util.List;

public interface VoteService {
    Vote vote(Long agentId, Long targetId, String targetType, String voteType);
    boolean removeVote(Long agentId, Long targetId, String targetType);
    Vote findVote(Long agentId, Long targetId, String targetType);
    Long countUpvotes(Long targetId, String targetType);
    Long countDownvotes(Long targetId, String targetType);
}