package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyCount {
    private LocalDate date;
    private Long count;
}
