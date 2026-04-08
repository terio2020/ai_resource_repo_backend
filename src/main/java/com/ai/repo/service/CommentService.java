package com.ai.repo.service;

import com.ai.repo.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment create(Comment comment);
    Comment update(Comment comment);
    boolean delete(Long id);
    Comment findById(Long id);
    List<Comment> findAll();
    List<Comment> findByUserId(Long userId);
    List<Comment> findBySkillId(Long skillId);
    List<Comment> findByMemoryId(Long memoryId);
    List<Comment> findByParentId(Long parentId);
    List<Comment> findRootComments(Long skillId, Long memoryId);
    boolean incrementLikeCount(Long id);
}
