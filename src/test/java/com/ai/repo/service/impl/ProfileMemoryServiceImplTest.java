package com.ai.repo.service.impl;

import com.ai.repo.dto.ProfileMemoryItemRequest;
import com.ai.repo.dto.ProfileMemoryPayload;
import com.ai.repo.dto.ProfileMemoryResponse;
import com.ai.repo.entity.Memory;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.ProfileMemoryItemMapper;
import com.ai.repo.service.MemoryService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileMemoryServiceImplTest {
    @Mock
    private MemoryService memoryService;
    @Mock
    private MemoryMapper memoryMapper;
    @Mock
    private ProfileMemoryItemMapper itemMapper;

    private ProfileMemoryServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProfileMemoryServiceImpl();
        inject("memoryService", memoryService);
        inject("memoryMapper", memoryMapper);
        inject("profileMemoryItemMapper", itemMapper);
        inject("objectMapper", new ObjectMapper());
    }

    @Test
    void upsert_shouldPersistAgentAuthoredStructuredProfile() {
        Memory memory = profileMemory();
        memory.setId(null);
        Memory saved = profileMemory();
        saved.setId(9L);
        when(memoryService.upsert(memory)).thenReturn(saved);
        when(memoryService.findById(9L)).thenReturn(saved);

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
        when(memoryService.upsert(memory)).thenReturn(saved);
        when(memoryService.findById(9L)).thenReturn(saved);

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
        existing.setRevision(1);
        when(memoryMapper.selectByUserIdAndAgentIdAndClientKey(1L, 5L, "codex-user-profile"))
                .thenReturn(existing);

        Memory result = service.upsert(memory, payload(item("UPSERT", "zh-CN")));

        assertEquals(existing, result);
        verify(memoryService, never()).upsert(any());
        verify(itemMapper, never()).upsert(any());
    }

    @Test
    void upsert_shouldRejectStaleRevision() {
        Memory memory = profileMemory();
        Memory existing = profileMemory();
        existing.setRevision(3);
        when(memoryMapper.selectByUserIdAndAgentIdAndClientKey(1L, 5L, "codex-user-profile"))
                .thenReturn(existing);

        ProfileMemoryPayload payload = payload(item("UPSERT", "zh-CN"));
        payload.setRevision(2);

        assertThrows(RuntimeException.class, () -> service.upsert(memory, payload));
        verify(memoryService, never()).upsert(any());
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
        verify(memoryService, never()).upsert(any());
    }

    @Test
    void findByUserId_shouldReturnProfileMemoriesAndItems() {
        when(memoryMapper.selectProfileByUserId(1L)).thenReturn(List.of(profileMemory()));
        when(itemMapper.selectByUserId(1L)).thenReturn(List.of());

        ProfileMemoryResponse response = service.findByUserId(1L);
        assertEquals(1, response.getMemories().size());
        assertEquals(0, response.getItems().size());
    }

    private Memory profileMemory() {
        Memory memory = new Memory();
        memory.setId(9L);
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
