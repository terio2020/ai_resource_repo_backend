package com.ai.repo.service.impl;

import com.ai.repo.entity.Statistics;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.StatisticsMapper;
import com.ai.repo.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private StatisticsMapper statisticsMapper;

    @Override
    public Statistics create(Statistics statistics) {
        statisticsMapper.insert(statistics);
        return statistics;
    }

    @Override
    public boolean delete(Long id) {
        if (statisticsMapper.selectById(id) == null) {
            throw new BusinessException("Statistics not found");
        }
        return statisticsMapper.deleteById(id) > 0;
    }

    @Override
    public Statistics findById(Long id) {
        return statisticsMapper.selectById(id);
    }

    @Override
    public List<Statistics> findAll() {
        return statisticsMapper.selectAll();
    }

    @Override
    public List<Statistics> findByUserId(Long userId) {
        return statisticsMapper.selectByUserId(userId);
    }

    @Override
    public List<Statistics> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return statisticsMapper.selectByUserIdAndDateRange(userId, startDate, endDate);
    }

    @Override
    public List<Statistics> findByMetricType(String metricType) {
        return statisticsMapper.selectByMetricType(metricType);
    }

    @Override
    public List<Statistics> findByUserIdAndMetricType(Long userId, String metricType) {
        return statisticsMapper.selectByUserIdAndMetricType(userId, metricType);
    }
}
