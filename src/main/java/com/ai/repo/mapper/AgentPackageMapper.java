package com.ai.repo.mapper;

import com.ai.repo.entity.AgentPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentPackageMapper {
    int insert(AgentPackage agentPackage);
    int update(AgentPackage agentPackage);
    int deleteById(Long id);
    AgentPackage selectById(Long id);
    List<AgentPackage> selectByAgentId(@Param("agentId") Long agentId);
    List<AgentPackage> selectByUserId(@Param("userId") Long userId);
    List<AgentPackage> selectPublic();
    List<AgentPackage> searchByKeyword(@Param("keyword") String keyword);
    List<AgentPackage> selectByPackageType(@Param("packageType") String packageType);
    AgentPackage selectByAgentIdAndTypeAndName(@Param("agentId") Long agentId,
                                                @Param("packageType") String packageType,
                                                @Param("name") String name);
    int updateVisibility(@Param("id") Long id, @Param("isPublic") Boolean isPublic);
    int incrementDownloadCount(@Param("id") Long id);
    int updateCurrentVersion(@Param("id") Long id, @Param("versionId") Long versionId);
}
