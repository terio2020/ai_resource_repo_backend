package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.AdminOverviewResponse;
import com.ai.repo.dto.AgentStatusCount;
import com.ai.repo.dto.ActivityTrendItem;
import com.ai.repo.dto.DailyCount;
import com.ai.repo.dto.DownloadTrendItem;
import com.ai.repo.dto.TopAgentItem;
import com.ai.repo.security.RequireAdmin;
import com.ai.repo.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@Validated
@Tag(name = "Admin Dashboard API", description = "Admin-only statistics dashboard")
public class AdminDashboardController {

    @Resource
    private AdminDashboardService adminDashboardService;

    @GetMapping("/overview")
    @RequireAdmin
    @Operation(summary = "Platform overview", description = "Aggregated platform counts (users, agents, memories, skill repos, packages, comments, bug reports, recent signups)")
    public ResponseEntity<Result<AdminOverviewResponse>> getOverview() {
        return Result.ok(adminDashboardService.getOverview());
    }

    @GetMapping("/user-growth")
    @RequireAdmin
    @Operation(summary = "User growth trend", description = "Daily new user registrations for the given number of days, missing dates filled with 0")
    public ResponseEntity<Result<List<DailyCount>>> getUserGrowth(
            @Parameter(description = "Number of days, default 30, max 90")
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        return Result.ok(adminDashboardService.getUserGrowth(days));
    }

    @GetMapping("/activity")
    @RequireAdmin
    @Operation(summary = "Activity trend", description = "Daily logins / new memories / new agents (approx), missing dates filled with 0")
    public ResponseEntity<Result<List<ActivityTrendItem>>> getActivity(
            @Parameter(description = "Number of days, default 30, max 90")
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        return Result.ok(adminDashboardService.getActivity(days));
    }

    @GetMapping("/downloads")
    @RequireAdmin
    @Operation(summary = "Download trend", description = "Daily download counts by type (memory/package/repo), missing dates filled with 0")
    public ResponseEntity<Result<List<DownloadTrendItem>>> getDownloads(
            @Parameter(description = "Number of days, default 30, max 90")
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        return Result.ok(adminDashboardService.getDownloads(days));
    }

    @GetMapping("/agents")
    @RequireAdmin
    @Operation(summary = "Agent status distribution", description = "Agent counts grouped by status")
    public ResponseEntity<Result<List<AgentStatusCount>>> getAgentStatusDistribution() {
        return Result.ok(adminDashboardService.getAgentStatusDistribution());
    }

    @GetMapping("/top-agents")
    @RequireAdmin
    @Operation(summary = "Top agents", description = "Agents ranked by memory count with aggregated like/download counts")
    public ResponseEntity<Result<List<TopAgentItem>>> getTopAgents(
            @Parameter(description = "Result limit, default 10, max 50")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return Result.ok(adminDashboardService.getTopAgents(limit));
    }
}
