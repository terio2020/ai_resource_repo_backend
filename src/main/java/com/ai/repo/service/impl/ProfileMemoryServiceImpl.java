package com.ai.repo.service.impl;

import com.ai.repo.dto.ProfileMemoryItemRequest;
import com.ai.repo.dto.ProfileMemoryGovernRequest;
import com.ai.repo.dto.ProfileMemoryPayload;
import com.ai.repo.dto.ProfileMemoryQuery;
import com.ai.repo.dto.ProfileMemoryResponse;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.ProfileMemoryGrant;
import com.ai.repo.entity.ProfileMemoryItem;
import com.ai.repo.entity.ProfileMemoryItemHistory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.ProfileMemoryGrantMapper;
import com.ai.repo.mapper.ProfileMemoryItemMapper;
import com.ai.repo.mapper.ProfileMemoryItemHistoryMapper;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
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
    private ProfileMemoryGrantMapper profileMemoryGrantMapper;
    @Resource
    private ProfileMemoryItemHistoryMapper profileMemoryItemHistoryMapper;
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
        memory.setProfileRequestHash(hash(serialize(Arrays.asList(
                memory.getUserId(), memory.getAgentId(), memory.getClientMemoryKey(),
                memory.getTitle(), memory.getContent(), memory.getVersion(),
                memory.getDescription(), memory.getFilePath(), memory.getFileSize(),
                memory.getMimeType(), memory.getTags(), memory.getCategory(),
                memory.getIsPublic(), memory.getStatus(), memory.getMetadata(),
                memory.getMemoryType(), memory.getSharingScope(), memory.getOwnerType(),
                schemaVersion, revision, profile.getItems()))));
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
                return acceptIdenticalReplay(memory, current);
            }

            memory.setId(current.getId());
            int updated = memoryMapper.updateProfileIfRevisionOlder(memory);
            if (updated != 1) {
                Memory latest = memoryMapper.selectProfileByKeyForUpdate(
                        memory.getUserId(), memory.getAgentId(), memory.getClientMemoryKey());
                if (latest != null && revision == latest.getRevision()) {
                    return acceptIdenticalReplay(memory, latest);
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

    private Memory acceptIdenticalReplay(Memory incoming, Memory stored) {
        if (stored.getProfileRequestHash() != null
                && Objects.equals(incoming.getProfileRequestHash(), stored.getProfileRequestHash())) {
            return stored;
        }
        throw new BusinessException(409, "Profile revision conflicts with the stored request");
    }

    @Override
    public ProfileMemoryResponse findByUserId(Long userId) {
        return new ProfileMemoryResponse(
                memoryMapper.selectProfileByUserId(userId),
                profileMemoryItemMapper.selectByUserId(userId));
    }

    @Override
    public ProfileMemoryResponse findByUserIdVisibleToAgent(Long userId, Long agentId) {
        return profileResponseFor(userId, profileMemoryItemMapper.selectByUserIdVisibleToAgent(userId, agentId));
    }

    @Override
    public ProfileMemoryResponse findByUserIdVisibleToAgent(Long userId, Long agentId, ProfileMemoryQuery query) {
        ProfileMemoryQuery safeQuery = normalizeQuery(query);
        List<ProfileMemoryItem> items = profileMemoryItemMapper.selectByUserIdVisibleToAgentQuery(userId, agentId, safeQuery);
        return profileResponseFor(userId, items);
    }

    private ProfileMemoryResponse profileResponseFor(Long userId, List<ProfileMemoryItem> items) {
        Set<Long> visibleMemoryIds = new LinkedHashSet<>();
        for (ProfileMemoryItem item : items) {
            visibleMemoryIds.add(item.getMemoryId());
        }
        List<Memory> memories = memoryMapper.selectProfileByUserId(userId).stream()
                .filter(memory -> visibleMemoryIds.contains(memory.getId()))
                .map(this::redactProfileMemoryForAgent)
                .toList();
        return new ProfileMemoryResponse(memories, items);
    }

    private ProfileMemoryQuery normalizeQuery(ProfileMemoryQuery query) {
        if (query == null) return ProfileMemoryQuery.empty();
        Integer maxItems = query.getMaxItems();
        if (maxItems != null && (maxItems < 1 || maxItems > 200)) {
            throw new BusinessException(400, "maxItems must be between 1 and 200");
        }
        List<String> namespaces = query.getNamespaces() == null ? null : query.getNamespaces().stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
        return new ProfileMemoryQuery(namespaces, maxItems);
    }

    /**
     * Profile parent rows are an implementation envelope. An Agent receives
     * structured items only; free-text parent fields could contain unrelated
     * profile dimensions and would bypass namespace grants.
     */
    private Memory redactProfileMemoryForAgent(Memory source) {
        Memory safe = new Memory();
        safe.setId(source.getId());
        safe.setUid(source.getUid());
        safe.setUserId(source.getUserId());
        safe.setAgentId(source.getAgentId());
        safe.setMemoryType(source.getMemoryType());
        safe.setSharingScope(source.getSharingScope());
        safe.setOwnerType(source.getOwnerType());
        safe.setSchemaVersion(source.getSchemaVersion());
        safe.setRevision(source.getRevision());
        safe.setVersion(source.getVersion());
        safe.setStatus(source.getStatus());
        safe.setCreatedAt(source.getCreatedAt());
        safe.setUpdatedAt(source.getUpdatedAt());
        return safe;
    }

    @Override
    @Transactional
    public ProfileMemoryItem governItem(Long userId, Long itemId, ProfileMemoryGovernRequest request) {
        ProfileMemoryItem item = profileMemoryItemMapper.selectByIdForUpdate(userId, itemId);
        if (item == null) {
            throw new BusinessException(404, "Profile item not found");
        }
        String action = request.getAction().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("CONFIRM", "CORRECT", "RETRACT", "RESOLVE").contains(action)) {
            throw new BusinessException(400, "Unsupported profile governance action");
        }
        if (containsSecret(request.getNote())) {
            throw new BusinessException(400, "Profile governance notes must not contain credentials or secrets");
        }

        String previousValue = item.getValueJson();
        String previousStatus = item.getStatus();
        List<ProfileMemoryItem> competingItems = "RESOLVE".equals(action)
                ? profileMemoryItemMapper.selectConflictsForUpdate(userId, item.getId(), item.getNamespace(),
                        item.getFactKey(), item.getContextHash())
                : List.of();
        if ("CORRECT".equals(action)) {
            if (request.getValue() == null) {
                throw new BusinessException(400, "CORRECT requires a value");
            }
            validateValueType(item.getValueType(), request.getValue());
            String correctedValue = serialize(request.getValue());
            if (containsSecret(correctedValue)) {
                throw new BusinessException(400, "Profile memories must not contain credentials or secrets");
            }
            item.setValueJson(correctedValue);
            item.setValueHash(hash(correctedValue));
            item.setStatus("CONFIRMED");
        } else if ("RETRACT".equals(action)) {
            item.setStatus("USER_RETRACTED");
        } else {
            item.setStatus("CONFIRMED");
        }

        if (profileMemoryItemMapper.updateGovernedItem(item) != 1) {
            throw new IllegalStateException("Profile item governance update failed");
        }
        if ("RESOLVE".equals(action)) {
            profileMemoryItemMapper.retractConflicts(userId, item.getId(), item.getNamespace(),
                    item.getFactKey(), item.getContextHash());
            for (ProfileMemoryItem competingItem : competingItems) {
                appendHistory(userId, competingItem, "RESOLVE_RETRACTED", competingItem.getValueJson(),
                        "USER_RETRACTED", request.getNote());
            }
        }
        appendHistory(userId, item, action, previousValue, item.getStatus(), request.getNote(), previousStatus);

        return profileMemoryItemMapper.selectByIdForUpdate(userId, itemId);
    }

    @Override
    public List<ProfileMemoryItemHistory> findItemHistory(Long userId, Long itemId) {
        return profileMemoryItemHistoryMapper.selectByItemId(userId, itemId);
    }

    @Override
    public List<ProfileMemoryGrant> findGrants(Long userId) {
        return profileMemoryGrantMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public List<ProfileMemoryGrant> replaceGrants(Long userId, Long agentId, List<String> namespaces) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String namespace : namespaces) {
            if (namespace == null || namespace.trim().length() > 100 || (!"*".equals(namespace.trim())
                    && !KEY_PATTERN.matcher(namespace.trim()).matches())) {
                throw new BusinessException(400, "Profile grant namespace contains unsupported characters");
            }
            normalized.add(namespace.trim());
        }
        profileMemoryGrantMapper.deleteByUserAndAgent(userId, agentId);
        for (String namespace : normalized) {
            ProfileMemoryGrant grant = new ProfileMemoryGrant();
            grant.setUid(UuidUtil.generate());
            grant.setUserId(userId);
            grant.setAgentId(agentId);
            grant.setNamespace(namespace);
            profileMemoryGrantMapper.insert(grant);
        }
        return profileMemoryGrantMapper.selectByUserId(userId);
    }

    private void appendHistory(Long userId, ProfileMemoryItem item, String action,
                               String previousValue, String newStatus, String note) {
        appendHistory(userId, item, action, previousValue, newStatus, note, item.getStatus());
    }

    private void appendHistory(Long userId, ProfileMemoryItem item, String action,
                               String previousValue, String newStatus, String note, String previousStatus) {
        ProfileMemoryItemHistory history = new ProfileMemoryItemHistory();
        history.setUid(UuidUtil.generate());
        history.setItemId(item.getId());
        history.setUserId(userId);
        history.setAction(action);
        history.setPreviousValueJson(previousValue);
        history.setNewValueJson(item.getValueJson());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setNote(note);
        profileMemoryItemHistoryMapper.insert(history);
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
