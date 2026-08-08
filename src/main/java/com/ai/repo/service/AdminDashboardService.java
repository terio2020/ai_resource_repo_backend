package com.ai.repo.service;

import com.ai.repo.dto.AdminOverviewResponse;
import com.ai.repo.dto.AgentStatusCount;
import com.ai.repo.dto.ActivityTrendItem;
import com.ai.repo.dto.DailyCount;
import com.ai.repo.dto.DownloadTrendItem;
import com.ai.repo.dto.TopAgentItem;

import java.util.List;

public interface AdminDashboardService {
    AdminOverviewResponse getOverview();
    List<DailyCount> getUserGrowth(int days);
    List<ActivityTrendItem> getActivity(int days);
    List<DownloadTrendItem> getDownloads(int days);
    List<AgentStatusCount> getAgentStatusDistribution();
    List<TopAgentItem> getTopAgents(int limit);
}
