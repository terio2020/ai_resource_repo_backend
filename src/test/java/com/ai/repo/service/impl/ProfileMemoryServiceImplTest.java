package com.ai.repo.service.impl;

import com.ai.repo.dto.ProfileMemoryItemRequest;
import com.ai.repo.dto.ProfileMemoryGovernRequest;
import com.ai.repo.dto.ProfileMemoryPayload;
import com.ai.repo.dto.ProfileMemoryQuery;
import com.ai.repo.dto.ProfileMemoryResponse;
import com.ai.repo.entity.Memory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.entity.ProfileMemoryItem;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.ProfileMemoryGrantMapper;
import com.ai.repo.mapper.ProfileMemoryItemHistoryMapper;
import com.ai.repo.mapper.ProfileMemoryItemMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileMemoryServiceImplTest {
    @Mock
    private MemoryMapper memoryMapper;
    @Mock
    private ProfileMemoryItemMapper itemMapper;
    @Mock
    private ProfileMemoryGrantMapper grantMapper;
    @Mock
    private ProfileMemoryItemHistoryMapper historyMapper;

    private ProfileMemoryServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProfileMemoryServiceImpl();
        inject("memoryMapper", memoryMapper);
        inject("profileMemoryItemMapper", itemMapper);
        inject("profileMemoryGrantMapper", grantMapper);
        inject("profileMemoryItemHistoryMapper", historyMapper);
        inject("objectMapper", new ObjectMapper());
    }

    @Test
    void upsert_shouldPersistAgentAuthoredStructuredProfile() {
        Memory memory = profileMemory();
        memory.setId(null);
        Memory saved = profileMemory();
        saved.setId(9L);
        when(memoryMapper.selectProfileByKeyForUpdate(1L, 5L, "codex-user-profile"))
                .thenReturn(saved);
        when(memoryMapper.selectById(9L)).thenReturn(saved);

        ProfileMemoryPayload payload = payload(item("UPSERT", "zh-CN"));
        Memory result = service.upsert(memory, payload);

        assertEquals(9L, result.getId());
        ArgumentCaptor<com.ai.repo.entity.ProfileMemoryItem> captor =
                ArgumentCaptor.forClass(com.ai.repo.entity.ProfileMemoryItem.class);
        verify(itemMapper).upsert(captor.capture());
        assertEquals("communication", captor.getValue().getNamespace());
        assertEquals("\"zh-CN\"", captor.getValue().getValueJson());
        assertNotNull(captor.getValue().getValueHash());
        verify(itemMapper).reconcileConflicts(1L);
    }

    @Test
    void upsert_shouldRetractByStableItemKey() {
        Memory memory = profileMemory();
        memory.setId(null);
        Memory saved = profileMemory();
        saved.setId(9L);
        when(memoryMapper.selectProfileByKeyForUpdate(1L, 5L, "codex-user-profile"))
                .thenReturn(saved);
        when(memoryMapper.selectById(9L)).thenReturn(saved);

        ProfileMemoryItemRequest request = new ProfileMemoryItemRequest();
        request.setItemKey("primary-language");
        request.setOperation("RETRACT");
        service.upsert(memory, payload(request));

        verify(itemMapper).retract(9L, "primary-language");
        verify(itemMapper, never()).upsert(any());
        verify(itemMapper).reconcileConflicts(1L);
    }

    @Test
    void upsert_shouldTreatSameRevisionAsIdempotentReplay() {
        Memory memory = profileMemory();
        memory.setRevision(null);
        Memory existing = profileMemory();
        existing.setUid("stored-profile-uid");
        existing.setRevision(1);
        when(memoryMapper.selectProfileByKeyForUpdate(1L, 5L, "codex-user-profile"))
                .thenAnswer(invocation -> {
                    existing.setProfileRequestHash(memory.getProfileRequestHash());
                    return existing;
                });

        Memory result = service.upsert(memory, payload(item("UPSERT", "zh-CN")));

        assertEquals(existing, result);
        verify(memoryMapper, never()).updateProfileIfRevisionOlder(any());
        verify(itemMapper, never()).upsert(any());
    }

    @Test
    void upsert_shouldRejectDifferentPayloadWithSameRevision() {
        Memory memory = profileMemory();
        Memory existing = profileMemory();
        existing.setUid("stored-profile-uid");
        existing.setRevision(1);
        existing.setProfileRequestHash("hash-of-first-payload");
        when(memoryMapper.selectProfileByKeyForUpdate(1L, 5L, "codex-user-profile"))
                .thenReturn(existing);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.upsert(memory, payload(item("UPSERT", "English"))));

        assertEquals(409, error.getCode());
        verify(memoryMapper, never()).updateProfileIfRevisionOlder(any());
        verify(itemMapper, never()).upsert(any());
    }

    @Test
    void upsert_shouldRejectLegacySameRevisionWithoutFingerprint() {
        Memory memory = profileMemory();
        Memory existing = profileMemory();
        existing.setUid("stored-profile-uid");
        existing.setRevision(1);
        when(memoryMapper.selectProfileByKeyForUpdate(1L, 5L, "codex-user-profile"))
                .thenReturn(existing);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.upsert(memory, payload(item("UPSERT", "zh-CN"))));

        assertEquals(409, error.getCode());
    }

    @Test
    void upsert_shouldRejectStaleRevision() {
        Memory memory = profileMemory();
        Memory existing = profileMemory();
        existing.setUid("stored-profile-uid");
        existing.setRevision(3);
        when(memoryMapper.selectProfileByKeyForUpdate(1L, 5L, "codex-user-profile"))
                .thenReturn(existing);

        ProfileMemoryPayload payload = payload(item("UPSERT", "zh-CN"));
        payload.setRevision(2);

        assertThrows(RuntimeException.class, () -> service.upsert(memory, payload));
        verify(memoryMapper, never()).updateProfileIfRevisionOlder(any());
    }

    @Test
    void upsert_shouldConditionallyAdvanceRevisionBeforeWritingItems() {
        Memory memory = profileMemory();
        Memory existing = profileMemory();
        existing.setUid("stored-profile-uid");
        existing.setRevision(1);
        when(memoryMapper.selectProfileByKeyForUpdate(1L, 5L, "codex-user-profile"))
                .thenReturn(existing);
        when(memoryMapper.updateProfileIfRevisionOlder(memory)).thenReturn(1);
        when(memoryMapper.selectById(9L)).thenReturn(memory);

        ProfileMemoryPayload payload = payload(item("UPSERT", "English"));
        payload.setRevision(2);

        Memory result = service.upsert(memory, payload);

        assertEquals(2, result.getRevision());
        verify(memoryMapper).updateProfileIfRevisionOlder(memory);
        verify(itemMapper).upsert(any());
    }

    @Test
    void upsert_shouldRejectValueThatDoesNotMatchDeclaredType() {
        Memory memory = profileMemory();
        memory.setId(null);

        ProfileMemoryItemRequest request = item("UPSERT", 42);
        request.setValueType("string");

        assertThrows(RuntimeException.class, () -> service.upsert(memory, payload(request)));
    }

    @Test
    void upsert_shouldRejectSecretsBeforeSavingMemory() {
        Memory memory = profileMemory();
        memory.setContent("api_key=1234567890abcdef");

        assertThrows(RuntimeException.class, () -> service.upsert(memory, payload(item("UPSERT", "zh-CN"))));
        verify(memoryMapper, never()).insertProfileIfAbsent(any());
    }

    @Test
    void findByUserId_shouldReturnProfileMemoriesAndItems() {
        when(memoryMapper.selectProfileByUserId(1L)).thenReturn(List.of(profileMemory()));
        when(itemMapper.selectByUserId(1L)).thenReturn(List.of());

        ProfileMemoryResponse response = service.findByUserId(1L);
        assertEquals(1, response.getMemories().size());
        assertEquals(0, response.getItems().size());
    }

    @Test
    void findByUserIdVisibleToAgent_shouldRedactProfileParentText() {
        Memory parent = profileMemory();
        ProfileMemoryItem item = profileItem();
        item.setMemoryId(parent.getId());
        when(itemMapper.selectByUserIdVisibleToAgent(1L, 6L)).thenReturn(List.of(item));
        when(memoryMapper.selectProfileByUserId(1L)).thenReturn(List.of(parent));

        ProfileMemoryResponse response = service.findByUserIdVisibleToAgent(1L, 6L);

        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getMemories().size());
        assertNull(response.getMemories().get(0).getContent());
        assertNull(response.getMemories().get(0).getDescription());
        assertEquals(parent.getRevision(), response.getMemories().get(0).getRevision());
    }

    @Test
    void findByUserIdVisibleToAgent_shouldAcceptScopedQuery() {
        Memory parent = profileMemory();
        ProfileMemoryItem item = profileItem();
        item.setMemoryId(parent.getId());
        ProfileMemoryQuery query = new ProfileMemoryQuery(List.of("communication"), 20);
        when(itemMapper.selectByUserIdVisibleToAgentQuery(1L, 6L, query)).thenReturn(List.of(item));
        when(memoryMapper.selectProfileByUserId(1L)).thenReturn(List.of(parent));

        ProfileMemoryResponse response = service.findByUserIdVisibleToAgent(1L, 6L, query);

        assertEquals(1, response.getItems().size());
        verify(itemMapper).selectByUserIdVisibleToAgentQuery(1L, 6L, query);
    }

    @Test
    void findByUserIdVisibleToAgent_shouldRejectInvalidMaxItems() {
        assertThrows(RuntimeException.class, () -> service.findByUserIdVisibleToAgent(
                1L, 6L, new ProfileMemoryQuery(null, 201)));
        verify(itemMapper, never()).selectByUserIdVisibleToAgentQuery(anyLong(), anyLong(), any());
    }

    @Test
    void governItem_shouldCorrectValueAndWriteHistory() {
        ProfileMemoryItem item = profileItem();
        when(itemMapper.selectByIdForUpdate(1L, 7L)).thenReturn(item);
        when(itemMapper.updateGovernedItem(item)).thenReturn(1);
        ProfileMemoryGovernRequest request = new ProfileMemoryGovernRequest();
        request.setAction("CORRECT");
        request.setValue("English");

        ProfileMemoryItem result = service.governItem(1L, 7L, request);

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("\"English\"", result.getValueJson());
        verify(historyMapper).insert(org.mockito.ArgumentMatchers.argThat(history ->
                "CORRECT".equals(history.getAction())
                        && "\"zh-CN\"".equals(history.getPreviousValueJson())));
    }

    @Test
    void governItem_shouldResolveCompetingValues() {
        ProfileMemoryItem item = profileItem();
        item.setStatus("CONFLICTED");
        ProfileMemoryItem competing = profileItem();
        competing.setId(8L);
        competing.setStatus("CONFLICTED");
        when(itemMapper.selectByIdForUpdate(1L, 7L)).thenReturn(item);
        when(itemMapper.selectConflictsForUpdate(1L, 7L, "communication", "language.primary", "context-hash"))
                .thenReturn(List.of(competing));
        when(itemMapper.updateGovernedItem(item)).thenReturn(1);
        ProfileMemoryGovernRequest request = new ProfileMemoryGovernRequest();
        request.setAction("RESOLVE");

        service.governItem(1L, 7L, request);

        verify(itemMapper).retractConflicts(1L, 7L, "communication", "language.primary", "context-hash");
        verify(historyMapper).insert(org.mockito.ArgumentMatchers.argThat(history ->
                history.getItemId().equals(8L)
                        && "RESOLVE_RETRACTED".equals(history.getAction())
                        && "CONFLICTED".equals(history.getPreviousStatus())
                        && "USER_RETRACTED".equals(history.getNewStatus())));
        assertEquals("CONFIRMED", item.getStatus());
    }

    @Test
    void replaceGrants_shouldAcceptWildcardAndDeduplicateNamespaces() {
        service.replaceGrants(1L, 5L, List.of("*", "communication", "communication"));

        verify(grantMapper).deleteByUserAndAgent(1L, 5L);
        verify(grantMapper, org.mockito.Mockito.times(2)).insert(any());
    }

    private ProfileMemoryItem profileItem() {
        ProfileMemoryItem item = new ProfileMemoryItem();
        item.setId(7L);
        item.setMemoryId(9L);
        item.setUserId(1L);
        item.setSourceAgentId(5L);
        item.setNamespace("communication");
        item.setFactKey("language.primary");
        item.setContextHash("context-hash");
        item.setValueType("string");
        item.setValueJson("\"zh-CN\"");
        item.setValueHash("value-hash");
        item.setStatus("ACTIVE");
        return item;
    }

    private Memory profileMemory() {
        Memory memory = new Memory();
        memory.setId(9L);
        memory.setUid("incoming-profile-uid");
        memory.setUserId(1L);
        memory.setAgentId(5L);
        memory.setTitle("Profile");
        memory.setContent("User prefers Chinese");
        memory.setMemoryType("USER_PROFILE");
        memory.setClientMemoryKey("codex-user-profile");
        return memory;
    }

    private ProfileMemoryPayload payload(ProfileMemoryItemRequest request) {
        ProfileMemoryPayload payload = new ProfileMemoryPayload();
        payload.setItems(List.of(request));
        return payload;
    }

    private ProfileMemoryItemRequest item(String operation, Object value) {
        ProfileMemoryItemRequest request = new ProfileMemoryItemRequest();
        request.setItemKey("primary-language");
        request.setOperation(operation);
        request.setNamespace("communication");
        request.setKey("language.primary");
        request.setValueType("string");
        request.setValue(value);
        request.setContext(Map.of("scope", "GLOBAL"));
        request.setRecordType("PREFERENCE");
        return request;
    }

    private void inject(String name, Object value) throws Exception {
        Field field = ProfileMemoryServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
