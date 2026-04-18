package com.ai.repo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Statistics {
    private Long id;
    private Long userId;
    private String metricType;
    private String metricName;
    private BigDecimal metricValue;
    private String unit;
    private LocalDate date;
    private LocalDateTime createdAt;
}