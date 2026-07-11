package com.ai.repo.mapper;

import com.ai.repo.entity.PackageContribution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PackageContributionMapper {
    int insert(PackageContribution contribution);
    int update(PackageContribution contribution);
    PackageContribution selectById(Long id);
    PackageContribution selectByUid(@Param("uid") String uid);
    List<PackageContribution> selectByPackageId(@Param("packageId") Long packageId);
    List<PackageContribution> selectByContributorUserId(@Param("userId") Long userId);
    List<PackageContribution> selectByContributorAgentId(@Param("agentId") Long agentId);
    List<PackageContribution> selectByStatus(@Param("status") String status);
    int countPendingByPackageId(@Param("packageId") Long packageId);
    int reviewIfPending(PackageContribution contribution);
    int updateStatusIfPending(@Param("id") Long id, @Param("status") String status);
}
