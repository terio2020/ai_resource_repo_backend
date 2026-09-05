package com.ai.repo.service.impl;

import com.ai.repo.dto.ProfileMemoryItemRequest;
import com.ai.repo.dto.ProfileMemoryPayload;
import com.ai.repo.dto.ProfileMemoryResponse;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.ProfileMemoryItem;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.ProfileMemoryItemMapper;
import com.ai.repo.service.ProfileMemoryService;
import com.ai.repo.util.UuidUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ProfileMemoryServiceImpl implements ProfileMemoryService {
    private static final Set<String> VALUE_TYPES = Set.of("string", "boolean", "number", "object", "array");
    private static final Set<String> RECORD_TYPES = Set.of("FACT", "PREFERENCE", "INSTRUCTION", "CONSTRAINT", "RELATION");
    private static final Set<String> SENSITIVITIES = Set.of("NORMAL", "PERSONAL", "SENSITIVE");
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9._-]{0,199}");
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|"
                    + "(?:api[_-]?key|password|passwd|secret|access[_-]?token|refresh[_-]?token)\\s*[:=]\\s*[^\\s,;]{8,}|"
                    + "\\bsk-[A-Za-z0-9_-]{16,})");

    @Resource
    private MemoryMapper memoryMapper;
    @Resource
    private ProfileMemoryItemMapper profileMemoryItemMapper;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public Memory upsert(Memory memory, ProfileMemoryPayload profile) {
        if (profile == null || profile.getItems() == null || profile.getItems().isEmpty()) {
            throw new BusinessException(400, "USER_PROFILE memories require profile items");
        }
        if (!"PATCH".equalsIgnoreCase(profile.getMode())) {
            throw new BusinessException(400, "Only PATCH profile mode is supported");
        }
        String schemaVersion = profile.getSchemaVersion() == null ? "1.0" : profile.getSchemaVersion();
        if (!"1.0".equals(schemaVersion)) {
            throw new BusinessException(400, "Unsupported profile schemaVersion");
        }
        if (memory.getClientMemoryKey() == null || memory.getClientMemoryKey().isBlank()) {
            throw new BusinessException(400, "USER_PROFILE memories require clientMemoryKey");
        }
        if (containsSecret(memory.getTitle()) || containsSecret(memory.getContent())
                || containsSecret(memory.getDescription()) || containsSecret(memory.getMetadata())) {
            throw new BusinessException(400, "Profile memories must not contain credentials or secrets");
        }

        int revision = profile.getRevision() == null ? 1 : profile.getRevision();
        if (revision < 1) {
            throw new BusinessException(400, "Profile revision must be positive");
        }
        // Validate the complete patch before creating or updating its parent
        // Memory. The transaction remains the final safeguard, but malformed
        // profile items should fail before any persistence call.
        for (ProfileMemoryItemRequest request : profile.getItems()) {
            validateItemRequest(request);
        }

        memory.setSchemaVersion(schemaVersion);
        memory.setRevision(revision);
        if (memory.getUid() == null || memory.getUid().isBlank()) {
            memory.setUid(UuidUtil.generate());
        }

        // The insert/no-op write and locking read serialize both first-create
        // and update races on (user_id, agent_id, client_memory_key). The UID
        // distinguishes a row inserted by this request from an existing row
        // without relying on connector-specific affected-row semantics.
        memoryMapper.insertProfileIfAbsent(memory);
        Memory current = memoryMapper.selectProfileByKeyForUpdate(
                memory.getUserId(), memory.getAgentId(), memory.getClientMemoryKey());
        if (current == null) {
            throw new IllegalStateException("Profile parent row was not created or found");
        }

        Memory saved;
        if (memory.getUid().equals(current.getUid())) {
            saved = current;
        } else {
            int storedRevision = current.getRevision() == null ? 1 : current.getRevision();
            if (revision < storedRevision) {
                throw new BusinessException(409, "Profile revision is older than the stored revision");
            }
            if (revision == storedRevision) {
                return current;
            }

            memory.setId(current.getId());
            int updated = memoryMapper.updateProfileIfRevisionOlder(memory);
            if (updated != 1) {
                Memory latest = memoryMapper.selectProfileByKeyForUpdate(
                        memory.getUserId(), memory.getAgentId(), memory.getClientMemoryKey());
                if (latest != null && revision == latest.getRevision()) {
                    return latest;
                }
                throw new BusinessException(409, "Profile revision was superseded by a concurrent update");
            }
            saved = memoryMapper.selectById(current.getId());
            if (saved == null) {
                throw new IllegalStateException("Updated profile parent row was not found");
            }
        }

        for (ProfileMemoryItemRequest request : profile.getItems()) {
            if (request.isRetract()) {
                profileMemoryItemMapper.retract(saved.getId(), request.getItemKey().trim());
                continue;
            }
            ProfileMemoryItem item = toEntity(saved, request);
            profileMemoryItemMapper.upsert(item);
        }
        profileMemoryItemMapper.reconcileConflicts(saved.getUserId());
        Memory refreshed = memoryMapper.selectById(saved.getId());
        return refreshed != null ? refreshed : saved;
    }

    @Override
    public ProfileMemoryResponse findByUserId(Long userId) {
        return new ProfileMemoryResponse(
                memoryMapper.selectProfileByUserId(userId),
                profileMemoryItemMapper.selectByUserId(userId));
    }

    private ProfileMemoryItem toEntity(Memory memory, ProfileMemoryItemRequest request) {
        String namespace = request.getNamespace().trim();
        String factKey = request.getKey().trim();
        String valueType = request.getValueType().toLowerCase(Locale.ROOT);
        String recordType = request.getRecordType().toUpperCase(Locale.ROOT);
        String sensitivity = request.getSensitivity() == null
                ? "NORMAL" : request.getSensitivity().toUpperCase(Locale.ROOT);

        String valueJson = serialize(request.getValue());
        String contextJson = request.getContext() == null ? "{}" : serialize(request.getContext());

        ProfileMemoryItem item = new ProfileMemoryItem();
        item.setUid(UuidUtil.generate());
        item.setMemoryId(memory.getId());
        item.setUserId(memory.getUserId());
        item.setSourceAgentId(memory.getAgentId());
        item.setItemKey(request.getItemKey().trim());
        item.setNamespace(namespace);
        item.setFactKey(factKey);
        item.setValueType(valueType);
        item.setValueJson(valueJson);
        item.setValueHash(hash(valueJson));
        item.setContextJson(contextJson);
        item.setContextHash(hash(contextJson));
        item.setRecordType(recordType);
        item.setConfidence(request.getConfidence());
        item.setSensitivity(sensitivity);
        item.setStatus("ACTIVE");
        item.setObservedAt(request.getObservedAt());
        item.setValidUntil(request.getValidUntil());
        return item;
    }

    private void validateItemRequest(ProfileMemoryItemRequest request) {
        try {
            request.validateUpsert();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, e.getMessage());
        }
        if (request.isRetract()) {
            return;
        }
        String namespace = request.getNamespace().trim();
        String factKey = request.getKey().trim();
        if (!KEY_PATTERN.matcher(namespace).matches() || !KEY_PATTERN.matcher(factKey).matches()) {
            throw new BusinessException(400, "Profile namespace and key contain unsupported characters");
        }
        String valueType = request.getValueType().toLowerCase(Locale.ROOT);
        String recordType = request.getRecordType().toUpperCase(Locale.ROOT);
        String sensitivity = request.getSensitivity() == null
                ? "NORMAL" : request.getSensitivity().toUpperCase(Locale.ROOT);
        if (!VALUE_TYPES.contains(valueType) || !RECORD_TYPES.contains(recordType)
                || !SENSITIVITIES.contains(sensitivity)) {
            throw new BusinessException(400, "Unsupported profile valueType, recordType, or sensitivity");
        }
        validateValueType(valueType, request.getValue());
        if (request.getContext() != null && !(request.getContext() instanceof Map)) {
            throw new BusinessException(400, "Profile context must be a JSON object");
        }
        if (request.getObservedAt() != null && request.getValidUntil() != null
                && !request.getValidUntil().isAfter(request.getObservedAt())) {
            throw new BusinessException(400, "Profile validUntil must be after observedAt");
        }
        String serializedValue = serialize(request.getValue());
        String serializedContext = request.getContext() == null ? "{}" : serialize(request.getContext());
        if (containsSecret(serializedValue) || containsSecret(serializedContext)) {
            throw new BusinessException(400, "Profile memories must not contain credentials or secrets");
        }
    }

    private boolean containsSecret(String value) {
        return value != null && SECRET_PATTERN.matcher(value).find();
    }

    private void validateValueType(String valueType, Object value) {
        boolean valid = switch (valueType) {
            case "string" -> value instanceof String;
            case "boolean" -> value instanceof Boolean;
            case "number" -> value instanceof Number;
            case "object" -> value instanceof Map;
            case "array" -> value instanceof List;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(400, "Profile value does not match valueType");
        }
    }

    private String serialize(Object value) {
        try {
            ObjectMapper canonical = objectMapper.copy()
                    .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return canonical.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "Profile value or context is not valid JSON");
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
