package com.ai.repo.service.impl;

import com.ai.repo.exception.ContentModerationException;
import com.ai.repo.exception.ContentModerationException.ModerationErrorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OpenAIModerationService {

    private static final String MODERATION_URL = "https://api.openai.com/v1/moderations";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${moderation.openai.api-key:}")
    private String apiKey;

    @Value("${moderation.openai.min-score:0.7}")
    private double minScore;

    public OpenAIModerationService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void moderateContent(String content, String fileName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API Key 未配置，跳过AI内容审核");
            return;
        }

        log.debug("开始OpenAI内容审核，文件名: {}", fileName);

        try {
            String jsonPayload = "{\"input\": \"" + escapeJson(content) + "\"}";

            Request request = new Request.Builder()
                    .url(MODERATION_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    log.error("OpenAI API 请求失败，状态码: {}, 响应: {}", response.code(), responseBody);
                    throw new ContentModerationException(ModerationErrorType.MODERATION_API_ERROR);
                }

                JsonNode resultNode = objectMapper.readTree(responseBody).get("results").get(0);

                boolean flagged = resultNode.get("flagged").asBoolean();
                if (flagged) {
                    JsonNode categories = resultNode.get("categories");
                    StringBuilder flaggedCategories = new StringBuilder();
                    categories.fieldNames().forEachRemaining(field -> {
                        if (categories.get(field).asBoolean()) {
                            if (flaggedCategories.length() > 0) {
                                flaggedCategories.append(", ");
                            }
                            flaggedCategories.append(field);
                        }
                    });

                    log.warn("OpenAI检测到敏感内容，文件: {}, 类别: {}", fileName, flaggedCategories);
                    throw new ContentModerationException(ModerationErrorType.SENSITIVE_CONTENT,
                            "检测到敏感类别: " + flaggedCategories);
                }

                JsonNode scores = resultNode.get("category_scores");
                scores.fieldNames().forEachRemaining(field -> {
                    double score = scores.get(field).asDouble();
                    if (score > (1 - minScore)) {
                        log.debug("检测到高风险类别 {}，分数: {}", field, score);
                    }
                });

                log.debug("OpenAI内容审核通过");
            }

        } catch (ContentModerationException e) {
            throw e;
        } catch (IOException e) {
            log.error("OpenAI API 调用异常，文件: {}", fileName, e);
            throw new ContentModerationException(ModerationErrorType.MODERATION_API_ERROR);
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}