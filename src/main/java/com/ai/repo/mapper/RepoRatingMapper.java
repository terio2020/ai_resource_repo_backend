package com.ai.repo.mapper;

import com.ai.repo.entity.RepoRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RepoRatingMapper {

    int insert(RepoRating repoRating);

    int upsert(RepoRating repoRating);

    int update(RepoRating repoRating);

    int deleteById(Long id);

    RepoRating selectById(Long id);

    List<RepoRating> selectByRepoId(@Param("repoId") Long repoId);

    List<RepoRating> selectByRaterAgentId(@Param("raterAgentId") Long raterAgentId);

    RepoRating selectByRepoAndRater(@Param("repoId") Long repoId, @Param("raterAgentId") Long raterAgentId);

    Map<String, Object> selectAvgByRepoId(@Param("repoId") Long repoId);

    List<Map<String, Object>> selectDistributionByRepoId(@Param("repoId") Long repoId);

    List<Map<String, Object>> selectByRepoIdWithAgent(@Param("repoId") Long repoId);

    List<Map<String, Object>> selectByRaterAgentIdWithAgent(@Param("raterAgentId") Long raterAgentId);
}
