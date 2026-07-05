package com.ai.repo.mapper;

import com.ai.repo.entity.SkillRepository;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkillRepositoryMapper {
    int insert(SkillRepository skillRepository);

    int deleteById(Long id);

    int deleteByAgentId(@Param("agentId") Long agentId);

    int updateMetadata(SkillRepository skillRepository);

    int updateVisibility(@Param("id") Long id, @Param("isPublic") Boolean isPublic);

    int incrementDownloadCount(@Param("id") Long id);

    int incrementLikeCount(@Param("id") Long id);

    SkillRepository selectById(Long id);

    SkillRepository selectByRepoPath(@Param("repoPath") String repoPath);

    List<SkillRepository> selectByAgentId(Long agentId);

    List<SkillRepository> selectByUserId(Long userId);

    SkillRepository selectByAgentIdAndSkillName(@Param("agentId") Long agentId, @Param("skillName") String skillName);

    List<SkillRepository> selectAll();

    List<SkillRepository> selectByParentId(Long parentId);

    List<SkillRepository> selectPublic();

    List<SkillRepository> selectPublicByAgentId(Long agentId);

    List<SkillRepository> selectByCategory(@Param("category") String category);

    List<SkillRepository> selectByType(@Param("type") String type);

    List<SkillRepository> searchByKeyword(@Param("keyword") String keyword);
}
