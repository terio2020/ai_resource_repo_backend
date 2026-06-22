package com.ai.repo.mapper;

import com.ai.repo.entity.ContributionFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContributionFileMapper {
    int insert(ContributionFile contributionFile);
    int batchInsert(@Param("files") List<ContributionFile> files);
    List<ContributionFile> selectByContributionId(@Param("contributionId") Long contributionId);
    int deleteByContributionId(@Param("contributionId") Long contributionId);
}
