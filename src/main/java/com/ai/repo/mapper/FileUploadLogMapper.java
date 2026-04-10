package com.ai.repo.mapper;

import com.ai.repo.entity.FileUploadLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileUploadLogMapper {
    int insert(FileUploadLog fileUploadLog);
    FileUploadLog selectById(Long id);
    List<FileUploadLog> selectByAgentIdAndFileType(@Param("agentId") Long agentId, @Param("fileType") String fileType);
    List<FileUploadLog> selectByUserId(@Param("userId") Long userId);
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
    Long countByAgentId(@Param("agentId") Long agentId, @Param("fileType") String fileType);
}