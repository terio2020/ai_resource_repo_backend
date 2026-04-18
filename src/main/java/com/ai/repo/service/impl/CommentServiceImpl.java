package com.ai.repo.service.impl;

import com.ai.repo.entity.Comment;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.CommentMapper;
import com.ai.repo.service.CommentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Override
    public Comment create(Comment comment) {
        commentMapper.insert(comment);

        if (comment.getParentId() != null) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null) {
                parent.setReplyCount(parent.getReplyCount() + 1);
                commentMapper.update(parent);
            }
        }

        return comment;
    }

    @Override
    public Comment update(Comment comment) {
        if (commentMapper.selectById(comment.getId()) == null) {
            throw new BusinessException("Comment not found");
        }
        commentMapper.update(comment);
        return comment;
    }

    @Override
    public boolean delete(Long id) {
        if (commentMapper.selectById(id) == null) {
            throw new BusinessException("Comment not found");
        }
        return commentMapper.deleteById(id) > 0;
    }

    @Override
    public Comment findById(Long id) {
        return commentMapper.selectById(id);
    }

    @Override
    public List<Comment> findAll() {
        return commentMapper.selectAll();
    }

    @Override
    public List<Comment> findByUserId(Long userId) {
        return commentMapper.selectByUserId(userId);
    }

    @Override
    public List<Comment> findBySkillId(Long skillId) {
        return commentMapper.selectBySkillId(skillId);
    }

    @Override
    public List<Comment> findByMemoryId(Long memoryId) {
        return commentMapper.selectByMemoryId(memoryId);
    }

    @Override
    public List<Comment> findByParentId(Long parentId) {
        return commentMapper.selectByParentId(parentId);
    }

    @Override
    public List<Comment> findRootComments(Long skillId, Long memoryId) {
        return commentMapper.selectRootComments(skillId, memoryId);
    }

    @Override
    public boolean incrementLikeCount(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("Comment not found");
        }
        comment.setLikeCount(comment.getLikeCount() + 1);
        return commentMapper.update(comment) > 0;
    }
}
