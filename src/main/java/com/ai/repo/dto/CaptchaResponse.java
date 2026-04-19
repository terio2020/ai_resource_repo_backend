package com.ai.repo.dto;

import lombok.Data;

@Data
public class CaptchaResponse {
    private String id;           // 验证ID
    private String puzzleImage;   // base64 拼图图片 (带验证块)
    private Integer targetY;      // 拼图块Y轴位置，用于前端滑动块对齐
}
