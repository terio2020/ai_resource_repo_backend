package com.ai.repo.service.impl;

import com.ai.repo.dto.AdminOverviewResponse;
import com.ai.repo.dto.AgentStatusCount;
import com.ai.repo.dto.ActivityTrendItem;
import com.ai.repo.dto.DailyCount;
import com.ai.repo.dto.DownloadTrendItem;
import com.ai.repo.dto.TopAgentItem;
import com.ai.repo.mapper.AdminDashboardMapper;
import com.ai.repo.service.AdminDashboardService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    @Resource
    private AdminDashboardMapper adminDashboardMapper;

    @Override
    public AdminOverviewResponse getOverview() {
        return adminDashboardMapper.selectOverview();
    }

    @Override
    public List<DailyCount> getUserGrowth(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);
        List<DailyCount> rows = adminDashboardMapper.selectUserGrowth(startDate);
        Map<LocalDate, Long> byDate = rows.stream()
                .collect(Collectors.toMap(DailyCount::getDate, DailyCount::getCount));
        List<DailyCount> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            DailyCount item = new DailyCount();
            item.setDate(date);
            item.setCount(byDate.getOrDefault(date, 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<ActivityTrendItem> getActivity(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);
        List<ActivityTrendItem> rows = adminDashboardMapper.selectActivity(startDate);
        return fillActivity(rows, startDate, days);
    }

    @Override
    public List<DownloadTrendItem> getDownloads(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);
        List<DownloadTrendItem> rows = adminDashboardMapper.selectDownloads(startDate);
        return fillDownloads(rows, startDate, days);
    }

    @Override
    public List<AgentStatusCount> getAgentStatusDistribution() {
        return adminDashboardMapper.selectAgentStatusDistribution();
    }

    @Override
    public List<TopAgentItem> getTopAgents(int limit) {
        return adminDashboardMapper.selectTopAgents(limit);
    }

    private List<ActivityTrendItem> fillActivity(List<ActivityTrendItem> rows, LocalDate startDate, int days) {
        Map<LocalDate, ActivityTrendItem> byDate = rows.stream()
                .collect(Collectors.toMap(ActivityTrendItem::getDate, Function.identity()));
        List<ActivityTrendItem> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            ActivityTrendItem item = byDate.get(date);
            if (item == null) {
                item = new ActivityTrendItem();
                item.setDate(date);
                item.setLogins(0L);
                item.setMemories(0L);
                item.setAgents(0L);
            }
            result.add(item);
        }
        return result;
    }

    private List<DownloadTrendItem> fillDownloads(List<DownloadTrendItem> rows, LocalDate startDate, int days) {
        Map<LocalDate, DownloadTrendItem> byDate = rows.stream()
                .collect(Collectors.toMap(DownloadTrendItem::getDate, Function.identity()));
        List<DownloadTrendItem> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            DownloadTrendItem item = byDate.get(date);
            if (item == null) {
                item = new DownloadTrendItem();
                item.setDate(date);
                item.setMemoryDownloads(0L);
                item.setPackageDownloads(0L);
                item.setRepoDownloads(0L);
            }
            result.add(item);
        }
        return result;
    }
}
