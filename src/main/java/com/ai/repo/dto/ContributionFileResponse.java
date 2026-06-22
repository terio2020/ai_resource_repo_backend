package com.ai.repo.dto;

import lombok.Data;

@Data
public class ContributionFileResponse {
    private Long id;
    private Long contributionId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String md5Hash;
    private String action;
}
