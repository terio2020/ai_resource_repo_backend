package com.ai.repo.mapper;

import com.ai.repo.entity.BugReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BugReportMapper {
    int insert(BugReport bugReport);
    int update(BugReport bugReport);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    int deleteById(Long id);
    BugReport selectById(Long id);
    BugReport selectByUid(@Param("uid") String uid);
    List<BugReport> selectAll();
    List<BugReport> selectByAgentId(Long agentId);
    List<BugReport> selectWithFilters(@Param("agentId") Long agentId,
                                       @Param("severity") String severity,
                                       @Param("status") String status,
                                       @Param("category") String category);
}