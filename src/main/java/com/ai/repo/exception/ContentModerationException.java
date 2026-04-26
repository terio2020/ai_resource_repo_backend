package com.ai.repo.exception;

import lombok.Getter;

@Getter
public class ContentModerationException extends BusinessException {

    private final ModerationErrorType errorType;

    public enum ModerationErrorType {
        IMAGE_NOT_ALLOWED("MD文件中不允许包含图片"),
        XSS_DETECTED("检测到潜在的XSS攻击内容"),
        SSRF_DETECTED("检测到内网访问尝试"),
        SENSITIVE_CONTENT("内容包含敏感信息，请修改后重试"),
        MODERATION_API_ERROR("内容审核服务异常，请稍后重试");

        private final String message;

        ModerationErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public ContentModerationException(ModerationErrorType errorType) {
        super(400, errorType.getMessage());
        this.errorType = errorType;
    }

    public ContentModerationException(ModerationErrorType errorType, String detail) {
        super(400, errorType.getMessage() + "：" + detail);
        this.errorType = errorType;
    }
}