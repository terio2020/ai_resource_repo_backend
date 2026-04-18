package com.ai.repo.mapper;

import com.ai.repo.entity.Skill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkillMapper {
    int insert(Skill skill);
    int update(Skill skill);
    int deleteById(Long id);
    Skill selectById(Long id);
    List<Skill> selectAll();
    List<Skill> selectByUserId(Long userId);
    List<Skill> selectByAgentId(Long agentId);
    List<Skill> selectByCategory(String category);
    List<Skill> selectByPublic(Boolean isPublic);
    List<Skill> searchByKeyword(String keyword);
    int batchDelete(@Param("ids") List<Long> ids);
    Skill selectByUserIdAndAgentIdAndName(@Param("userId") Long userId, @Param("agentId") Long agentId, @Param("name") String name);
    int updateByCompositeKey(Skill skill);
}
