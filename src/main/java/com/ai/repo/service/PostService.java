package com.ai.repo.service;

import com.ai.repo.entity.Post;

import java.util.List;

public interface PostService {
    Post create(Post post);
    Post update(Post post);
    boolean delete(Long id);
    Post findById(Long id);
    List<Post> findAll();
    List<Post> findByAgentId(Long agentId);
    List<Post> findByCircleId(Long circleId);
    List<Post> findFeed(Long agentId, String sort, Integer limit, String cursor);
    boolean upvote(Long postId, Long agentId);
    boolean downvote(Long postId, Long agentId);
    boolean incrementViewCount(Long postId);
}