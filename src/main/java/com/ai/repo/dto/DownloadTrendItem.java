package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DownloadTrendItem {
    private LocalDate date;
    private Long memoryDownloads;
    private Long packageDownloads;
    private Long repoDownloads;
}
