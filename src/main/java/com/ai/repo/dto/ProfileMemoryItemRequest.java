package com.ai.repo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProfileMemoryItemRequest {
    @NotBlank
    @Size(max = 200)
    private String itemKey;
    private String operation = "UPSERT";
    @Size(max = 100)
    private String namespace;
    @Size(max = 200)
    private String key;
    @Size(max = 30)
    private String valueType;
    private Object value;
    private Object context;
    @Size(max = 30)
    private String recordType;
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal confidence;
    @Size(max = 30)
    private String sensitivity;
    private LocalDateTime observedAt;
    private LocalDateTime validUntil;

    public boolean isRetract() {
        return "RETRACT".equalsIgnoreCase(operation);
    }

    public void validateUpsert() {
        if (!"UPSERT".equalsIgnoreCase(operation) && !"RETRACT".equalsIgnoreCase(operation)) {
            throw new IllegalArgumentException("Profile operation must be UPSERT or RETRACT");
        }
        if (!isRetract() && (namespace == null || namespace.isBlank()
                || key == null || key.isBlank()
                || valueType == null || valueType.isBlank()
                || value == null
                || recordType == null || recordType.isBlank())) {
            throw new IllegalArgumentException("Profile UPSERT items require namespace, key, valueType, value, and recordType");
        }
    }
}
