package com.ai.repo.mapper;

import com.ai.repo.entity.PackageVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PackageVersionMapper {
    int insert(PackageVersion packageVersion);
    PackageVersion selectById(Long id);
    List<PackageVersion> selectByPackageId(@Param("packageId") Long packageId);
    PackageVersion selectLatestByPackageId(@Param("packageId") Long packageId);
    int selectMaxVersionNum(@Param("packageId") Long packageId);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
