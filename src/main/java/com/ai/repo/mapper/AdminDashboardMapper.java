package com.ai.repo.mapper;

import com.ai.repo.dto.AdminOverviewResponse;
import com.ai.repo.dto.AgentStatusCount;
import com.ai.repo.dto.ActivityTrendItem;
import com.ai.repo.dto.DailyCount;
import com.ai.repo.dto.DownloadTrendItem;
import com.ai.repo.dto.TopAgentItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AdminDashboardMapper {
    AdminOverviewResponse selectOverview();
    List<DailyCount> selectUserGrowth(@Param("startDate") LocalDate startDate);
    List<ActivityTrendItem> selectActivity(@Param("startDate") LocalDate startDate);
    List<DownloadTrendItem> selectDownloads(@Param("startDate") LocalDate startDate);
    List<AgentStatusCount> selectAgentStatusDistribution();
    List<TopAgentItem> selectTopAgents(@Param("limit") int limit);
}
