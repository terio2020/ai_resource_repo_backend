package com.ai.repo.service.impl;

import com.ai.repo.entity.Vote;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.VoteMapper;
import com.ai.repo.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VoteServiceImpl implements VoteService {

    @Autowired
    private VoteMapper voteMapper;

    @Override
    @Transactional
    public Vote vote(Long agentId, Long targetId, String targetType, String voteType) {
        Vote existingVote = voteMapper.selectByTarget(agentId, targetId, targetType);
        
        if (existingVote != null) {
            if (voteType.equals(existingVote.getVoteType())) {
                return existingVote;
            }
            voteMapper.deleteById(existingVote.getId());
        }
        
        Vote vote = new Vote();
        vote.setAgentId(agentId);
        vote.setTargetId(targetId);
        vote.setTargetType(targetType);
        vote.setVoteType(voteType);
        voteMapper.insert(vote);
        
        return vote;
    }

    @Override
    @Transactional
    public boolean removeVote(Long agentId, Long targetId, String targetType) {
        Vote existingVote = voteMapper.selectByTarget(agentId, targetId, targetType);
        if (existingVote == null) {
            return false;
        }
        return voteMapper.deleteById(existingVote.getId()) > 0;
    }

    @Override
    public Vote findVote(Long agentId, Long targetId, String targetType) {
        return voteMapper.selectByTarget(agentId, targetId, targetType);
    }

    @Override
    public Long countUpvotes(Long targetId, String targetType) {
        return voteMapper.countUpvotes(targetId, targetType);
    }

    @Override
    public Long countDownvotes(Long targetId, String targetType) {
        return voteMapper.countDownvotes(targetId, targetType);
    }
}