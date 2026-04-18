package com.ai.repo.service.repository;

import com.ai.repo.entity.Post;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.PostMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostRepository {

    @Resource
    private PostMapper postMapper;

    @Resource
    private AgentMapper agentMapper;

    public Post findById(Long id) {
        return postMapper.selectById(id);
    }

    public Post save(Post post) {
        if (post.getId() == null) {
            postMapper.insert(post);
        return post;
        }
        postMapper.update(post);
        return post;
    }

    public List<Post> findByAgentId(Long agentId, Integer limit) {
        return postMapper.selectByAgentId(agentId);
    }

    public List<Post> findBySubscribedCircles(Long agentId, Integer limit) {
        return postMapper.selectPage(null, agentId, "new", limit, 0);
    }

    public List<Post> findRecommendedPosts(Integer limit) {
        return postMapper.selectPage(null, null, "hot", limit, 0);
    }
}