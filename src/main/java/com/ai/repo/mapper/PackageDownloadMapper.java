package com.ai.repo.mapper;

import com.ai.repo.entity.PackageDownload;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PackageDownloadMapper {
    int insert(PackageDownload download);
    List<PackageDownload> selectByPackageId(@Param("packageId") Long packageId);
    int countByPackageId(@Param("packageId") Long packageId);
    int countByVersionId(@Param("versionId") Long versionId);
}
