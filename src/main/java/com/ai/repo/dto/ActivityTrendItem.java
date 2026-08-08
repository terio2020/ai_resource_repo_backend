package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ActivityTrendItem {
    private LocalDate date;
    private Long logins;
    private Long memories;
    private Long agents;
}
