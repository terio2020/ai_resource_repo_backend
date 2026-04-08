package com.ai.repo.mapper;

import com.ai.repo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    int insert(Comment comment);
    int update(Comment comment);
    int deleteById(Long id);
    Comment selectById(Long id);
    List<Comment> selectAll();
    List<Comment> selectByUserId(Long userId);
    List<Comment> selectBySkillId(Long skillId);
    List<Comment> selectByMemoryId(Long memoryId);
    List<Comment> selectByParentId(Long parentId);
    List<Comment> selectRootComments(Long skillId, Long memoryId);
}
