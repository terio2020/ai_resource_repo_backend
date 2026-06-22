package com.ai.repo.mapper;

import com.ai.repo.entity.PackageFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PackageFileMapper {
    int insert(PackageFile packageFile);
    int batchInsert(@Param("files") List<PackageFile> files);
    PackageFile selectById(Long id);
    List<PackageFile> selectByVersionId(@Param("versionId") Long versionId);
    int deleteByVersionId(@Param("versionId") Long versionId);
}
