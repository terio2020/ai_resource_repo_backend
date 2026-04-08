package com.ai.repo.service;

import com.ai.repo.entity.Statistics;

import java.time.LocalDate;
import java.util.List;

public interface StatisticsService {
    Statistics create(Statistics statistics);
    boolean delete(Long id);
    Statistics findById(Long id);
    List<Statistics> findAll();
    List<Statistics> findByUserId(Long userId);
    List<Statistics> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    List<Statistics> findByMetricType(String metricType);
    List<Statistics> findByUserIdAndMetricType(Long userId, String metricType);
}
