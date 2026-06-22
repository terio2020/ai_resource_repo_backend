package com.ai.repo.service.impl;

import com.ai.repo.dto.*;
import com.ai.repo.entity.*;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.*;
import com.ai.repo.service.ContentModerationService;
import com.ai.repo.service.PackageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceImplTest {

    @Mock
    private AgentPackageMapper agentPackageMapper;
    @Mock
    private PackageVersionMapper packageVersionMapper;
    @Mock
    private PackageFileMapper packageFileMapper;
    @Mock
    private PackageDownloadMapper packageDownloadMapper;
    @Mock
    private PackageStorageService packageStorageService;
    @Mock
    private ContentModerationService contentModerationService;

    private PackageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PackageServiceImpl();
        ReflectionTestUtils.setField(service, "agentPackageMapper", agentPackageMapper);
        ReflectionTestUtils.setField(service, "packageVersionMapper", packageVersionMapper);
        ReflectionTestUtils.setField(service, "packageFileMapper", packageFileMapper);
        ReflectionTestUtils.setField(service, "packageDownloadMapper", packageDownloadMapper);
        ReflectionTestUtils.setField(service, "packageStorageService", packageStorageService);
        ReflectionTestUtils.setField(service, "contentModerationService", contentModerationService);
        ReflectionTestUtils.setField(service, "basePath", "/data/packages/");
    }

    private AgentPackage createPackage(Long id, Long userId, Long agentId, String name) {
        AgentPackage ap = new AgentPackage();
        ap.setId(id);
        ap.setUserId(userId);
        ap.setAgentId(agentId);
        ap.setName(name);
        ap.setPackageType("skill");
        ap.setIsPublic(false);
        ap.setDownloadCount(0);
        ap.setCreatedAt(LocalDateTime.now());
        return ap;
    }

    // ==================== create ====================

    @Test
    void create_shouldInsert_whenNotDuplicate() {
        PackageCreateRequest req = new PackageCreateRequest();
        req.setAgentId(10L);
        req.setPackageType("skill");
        req.setName("my-skill");
        req.setDescription("A test skill");

        when(agentPackageMapper.selectByAgentIdAndTypeAndName(10L, "skill", "my-skill")).thenReturn(null);

        AgentPackage result = service.create(1L, 10L, req);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("my-skill", result.getName());
        assertFalse(result.getIsPublic());
        verify(agentPackageMapper).insert(any(AgentPackage.class));
    }

    @Test
    void create_shouldThrow_whenDuplicate() {
        PackageCreateRequest req = new PackageCreateRequest();
        req.setAgentId(10L);
        req.setPackageType("skill");
        req.setName("my-skill");

        when(agentPackageMapper.selectByAgentIdAndTypeAndName(10L, "skill", "my-skill"))
                .thenReturn(createPackage(1L, 1L, 10L, "my-skill"));

        assertThrows(BusinessException.class, () -> service.create(1L, 10L, req));
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturn_whenExists() {
        AgentPackage ap = createPackage(1L, 1L, 10L, "test");
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        AgentPackage result = service.findById(1L);
        assertEquals("test", result.getName());
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(agentPackageMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.findById(99L));
    }

    // ==================== setVisibility ====================

    @Test
    void setVisibility_shouldUpdate_whenOwner() {
        AgentPackage ap = createPackage(1L, 1L, 10L, "test");
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        service.setVisibility(1L, 1L, true);

        verify(agentPackageMapper).updateVisibility(1L, true);
    }

    @Test
    void setVisibility_shouldThrow_whenNotOwner() {
        AgentPackage ap = createPackage(1L, 1L, 10L, "test");
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        assertThrows(BusinessException.class, () -> service.setVisibility(1L, 2L, true));
    }

    // ==================== rollback ====================

    @Test
    void rollback_shouldUpdateCurrentVersion() {
        AgentPackage ap = createPackage(1L, 1L, 10L, "test");
        ap.setCurrentVersionId(5L);
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        PackageVersion target = new PackageVersion();
        target.setId(3L);
        target.setPackageId(1L);
        when(packageVersionMapper.selectById(3L)).thenReturn(target);

        service.rollback(1L, 1L, 3L);

        verify(agentPackageMapper).updateCurrentVersion(1L, 3L);
    }

    @Test
    void rollback_shouldThrow_whenVersionNotInPackage() {
        AgentPackage ap = createPackage(1L, 1L, 10L, "test");
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        PackageVersion target = new PackageVersion();
        target.setId(3L);
        target.setPackageId(99L); // wrong package
        when(packageVersionMapper.selectById(3L)).thenReturn(target);

        assertThrows(BusinessException.class, () -> service.rollback(1L, 1L, 3L));
    }

    // ==================== delete ====================

    @Test
    void delete_shouldRemovePackageAndFiles() {
        AgentPackage ap = createPackage(1L, 1L, 10L, "test");
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        PackageVersion v1 = new PackageVersion();
        v1.setId(1L);
        v1.setStoragePath("/data/packages/v1");
        when(packageVersionMapper.selectByPackageId(1L)).thenReturn(List.of(v1));

        service.delete(1L, 1L);

        verify(packageStorageService).deleteDirectory("/data/packages/v1");
        verify(agentPackageMapper).deleteById(1L);
    }

    // ==================== search ====================

    @Test
    void search_shouldDelegateToMapper() {
        when(agentPackageMapper.searchByKeyword("weather")).thenReturn(
                List.of(createPackage(1L, 1L, 10L, "weather-skill"))
        );

        List<PackageResponse> results = service.search("weather");

        assertEquals(1, results.size());
        verify(agentPackageMapper).searchByKeyword("weather");
    }
}
