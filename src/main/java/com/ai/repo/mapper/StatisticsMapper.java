package com.ai.repo.mapper;

import com.ai.repo.entity.Statistics;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StatisticsMapper {
    int insert(Statistics statistics);
    int deleteById(Long id);
    Statistics selectById(Long id);
    List<Statistics> selectAll();
    List<Statistics> selectByUserId(Long userId);
    List<Statistics> selectByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    List<Statistics> selectByMetricType(String metricType);
    List<Statistics> selectByUserIdAndMetricType(Long userId, String metricType);
}
