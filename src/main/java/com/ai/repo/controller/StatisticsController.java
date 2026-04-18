package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.Statistics;
import com.ai.repo.service.StatisticsService;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @PostMapping
    public Result<Statistics> createStatistics(@RequestBody Statistics statistics) {
        Statistics createdStats = statisticsService.create(statistics);
        return Result.success(createdStats);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteStatistics(@PathVariable Long id) {
        statisticsService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Statistics> getStatisticsById(@PathVariable Long id) {
        Statistics statistics = statisticsService.findById(id);
        return Result.success(statistics);
    }

    @GetMapping
    public Result<List<Statistics>> getAllStatistics() {
        List<Statistics> statistics = statisticsService.findAll();
        return Result.success(statistics);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Statistics>> getStatisticsByUserId(@PathVariable Long userId) {
        List<Statistics> statistics = statisticsService.findByUserId(userId);
        return Result.success(statistics);
    }

    @GetMapping("/user/{userId}/range")
    public Result<List<Statistics>> getStatisticsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Statistics> statistics = statisticsService.findByUserIdAndDateRange(userId, startDate, endDate);
        return Result.success(statistics);
    }

    @GetMapping("/type/{metricType}")
    public Result<List<Statistics>> getStatisticsByMetricType(@PathVariable String metricType) {
        List<Statistics> statistics = statisticsService.findByMetricType(metricType);
        return Result.success(statistics);
    }

    @GetMapping("/user/{userId}/type/{metricType}")
    public Result<List<Statistics>> getStatisticsByUserIdAndMetricType(
            @PathVariable Long userId,
            @PathVariable String metricType) {
        List<Statistics> statistics = statisticsService.findByUserIdAndMetricType(userId, metricType);
        return Result.success(statistics);
    }
}
