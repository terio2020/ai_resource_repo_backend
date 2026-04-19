package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CaptchaVerifyRequest {
    @NotBlank(message = "验证ID不能为空")
    private String id;

    @NotNull(message = "滑动位置不能为空")
    private Integer moveX;

    private Integer moveY;  // 可选，支持Y轴验证
}
