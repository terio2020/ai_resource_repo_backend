package com.ai.repo.mapper;

import com.ai.repo.entity.SkillRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SkillRatingMapper {

    int insert(SkillRating skillRating);

    int upsert(SkillRating skillRating);

    int update(SkillRating skillRating);

    int deleteById(Long id);

    SkillRating selectById(Long id);

    List<SkillRating> selectBySkillId(@Param("skillId") Long skillId);

    List<SkillRating> selectByRaterAgentId(@Param("raterAgentId") Long raterAgentId);

    SkillRating selectBySkillAndRater(@Param("skillId") Long skillId, @Param("raterAgentId") Long raterAgentId);

    Map<String, Object> selectAvgBySkillId(@Param("skillId") Long skillId);

    List<Map<String, Object>> selectDistributionBySkillId(@Param("skillId") Long skillId);

    List<Map<String, Object>> selectBySkillIdWithAgent(@Param("skillId") Long skillId);

    List<Map<String, Object>> selectByRaterAgentIdWithAgent(@Param("raterAgentId") Long raterAgentId);
}
