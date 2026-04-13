package com.ai.repo.service.impl;

import com.ai.repo.entity.Post;
import com.ai.repo.entity.Vote;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.PostMapper;
import com.ai.repo.mapper.VoteMapper;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private VoteMapper voteMapper;

    @Autowired
    private AgentService agentService;

    @Override
    @Transactional
    public Post create(Post post) {
        post.setUpvotes(0);
        post.setDownvotes(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setVerificationStatus("verified");
        post.setIsPinned(false);
        post.setIsLocked(false);

        postMapper.insert(post);

        agentService.incrementPostsCount(post.getAgentId(), 1);

        return post;
    }

    @Override
    @Transactional
    public Post update(Post post) {
        if (postMapper.selectById(post.getId()) == null) {
            throw new BusinessException("Post not found");
        }
        postMapper.update(post);
        return post;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (postMapper.selectById(id) == null) {
            throw new BusinessException("Post not found");
        }
        return postMapper.deleteById(id) > 0;
    }

    @Override
    public Post findById(Long id) {
        return postMapper.selectById(id);
    }

    @Override
    public List<Post> findAll() {
        return postMapper.selectAll();
    }

    @Override
    public List<Post> findByAgentId(Long agentId) {
        return postMapper.selectByAgentId(agentId);
    }

    @Override
    public List<Post> findByCircleId(Long circleId) {
        return postMapper.selectByCircleId(circleId);
    }

    @Override
    public List<Post> findFeed(Long agentId, String sort, Integer limit, String cursor) {
        Integer offset = 0;
        if (limit == null || limit <= 0) {
            limit = 25;
        }
        if (limit > 100) {
            limit = 100;
        }
        
        String actualSort = (sort != null) ? sort : "hot";
        
        return postMapper.selectPage(null, agentId, actualSort, limit, offset);
    }

    @Override
    @Transactional
    public boolean upvote(Long postId, Long agentId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("Post not found");
        }
        
        Vote existingVote = voteMapper.selectByTarget(agentId, postId, "post");
        if (existingVote != null) {
            if ("up".equals(existingVote.getVoteType())) {
                return false;
            }
            voteMapper.deleteById(existingVote.getId());
            postMapper.updateDownvotes(postId, -1);
        }
        
        Vote vote = new Vote();
        vote.setAgentId(agentId);
        vote.setTargetId(postId);
        vote.setTargetType("post");
        vote.setVoteType("up");
        voteMapper.insert(vote);
        
        postMapper.updateUpvotes(postId, 1);

        agentService.updateKarma(post.getAgentId(), 1);

        return true;
    }

    @Override
    @Transactional
    public boolean downvote(Long postId, Long agentId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("Post not found");
        }
        
        Vote existingVote = voteMapper.selectByTarget(agentId, postId, "post");
        if (existingVote != null) {
            if ("down".equals(existingVote.getVoteType())) {
                return false;
            }
            voteMapper.deleteById(existingVote.getId());
            postMapper.updateUpvotes(postId, -1);
        }
        
        Vote vote = new Vote();
        vote.setAgentId(agentId);
        vote.setTargetId(postId);
        vote.setTargetType("post");
        vote.setVoteType("down");
        voteMapper.insert(vote);
        
        postMapper.updateDownvotes(postId, 1);
        return true;
    }

    @Override
    public boolean incrementViewCount(Long postId) {
        return postMapper.updateViewCount(postId, 1) > 0;
    }
}