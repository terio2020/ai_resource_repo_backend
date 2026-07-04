package com.ai.repo.service.impl;

import com.ai.repo.dto.ContributionResponse;
import com.ai.repo.dto.ContributionReviewRequest;
import com.ai.repo.entity.*;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.*;
import com.ai.repo.service.PackageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class PackageContributionServiceImplTest {

    @TempDir
    static Path tempRoot;

    @Mock
    private AgentPackageMapper agentPackageMapper;
    @Mock
    private PackageVersionMapper packageVersionMapper;
    @Mock
    private PackageFileMapper packageFileMapper;
    @Mock
    private PackageContributionMapper packageContributionMapper;
    @Mock
    private ContributionFileMapper contributionFileMapper;
    @Mock
    private PackageStorageService packageStorageService;

    private PackageContributionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PackageContributionServiceImpl();
        ReflectionTestUtils.setField(service, "basePath", tempRoot.toString() + "/packages/");
        ReflectionTestUtils.setField(service, "agentPackageMapper", agentPackageMapper);
        ReflectionTestUtils.setField(service, "packageVersionMapper", packageVersionMapper);
        ReflectionTestUtils.setField(service, "packageFileMapper", packageFileMapper);
        ReflectionTestUtils.setField(service, "packageContributionMapper", packageContributionMapper);
        ReflectionTestUtils.setField(service, "contributionFileMapper", contributionFileMapper);
        ReflectionTestUtils.setField(service, "packageStorageService", packageStorageService);
    }

    private AgentPackage createPublicPackage(Long id, Long userId, Long agentId) {
        AgentPackage ap = new AgentPackage();
        ap.setId(id);
        ap.setUserId(userId);
        ap.setAgentId(agentId);
        ap.setName("public-skill");
        ap.setPackageType("skill");
        ap.setIsPublic(true);
        ap.setCreatedAt(LocalDateTime.now());
        return ap;
    }

    private PackageVersion createVersion(Long id, Long packageId) {
        PackageVersion pv = new PackageVersion();
        pv.setId(id);
        pv.setPackageId(packageId);
        pv.setVersionTag("v1");
        pv.setStoragePath("/data/packages/v1");
        pv.setCreatedAt(LocalDateTime.now());
        return pv;
    }

    // ==================== submit ====================

    @Test
    void submit_shouldCreateContribution_whenValid() {
        AgentPackage ap = createPublicPackage(1L, 1L, 10L);
        PackageVersion source = createVersion(5L, 1L);

        when(agentPackageMapper.selectById(1L)).thenReturn(ap);
        when(packageVersionMapper.selectById(5L)).thenReturn(source);
        doAnswer((Answer<Void>) invocation -> {
            PackageContribution pc = invocation.getArgument(0);
            pc.setId(100L);
            return null;
        }).when(packageContributionMapper).insert(any(PackageContribution.class));
        when(packageStorageService.saveContributionFile(eq(100L), anyString(), any()))
                .thenReturn("/tmp/file.md");

        List<MultipartFile> files = List.of(new MockMultipartFile("files", "fix.md", "text/markdown", "fix".getBytes()));

        ContributionResponse response = service.submit(1L, 2L, null, 5L, "fix bug", files);

        assertNotNull(response);
        assertEquals("pending", response.getStatus());
        assertEquals(2L, response.getContributorUserId());
        verify(packageContributionMapper).insert(any(PackageContribution.class));
    }

    @Test
    void submit_shouldThrow_whenPrivatePackage() {
        AgentPackage ap = createPublicPackage(1L, 1L, 10L);
        ap.setIsPublic(false);
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        assertThrows(BusinessException.class, () ->
                service.submit(1L, 2L, null, 5L, "msg", List.of()));
    }

    @Test
    void submit_shouldThrow_whenContributorIsOwner() {
        AgentPackage ap = createPublicPackage(1L, 1L, 10L);
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        assertThrows(BusinessException.class, () ->
                service.submit(1L, 1L, null, 5L, "msg", List.of()));
    }

    // ==================== review (approve) ====================

    @Test
    void review_shouldApproveAndCreateVersion() throws Exception {
        AgentPackage ap = createPublicPackage(1L, 1L, 10L);
        PackageVersion source = createVersion(5L, 1L);

        PackageContribution pc = new PackageContribution();
        pc.setId(10L);
        pc.setPackageId(1L);
        pc.setSourceVersionId(5L);
        pc.setContributorUserId(2L);
        pc.setStatus("pending");

        // Create temp file for the contribution file merge
        java.nio.file.Path tempContribFile = java.nio.file.Files.createTempFile("contrib_", ".md");
        java.nio.file.Files.writeString(tempContribFile, "modified content");

        ContributionFile cf = new ContributionFile();
        cf.setFilePath("fix.md");
        cf.setStoragePath(tempContribFile.toAbsolutePath().toString());

        PackageFile mergedFile = new PackageFile();
        mergedFile.setVersionId(99L);
        mergedFile.setFileName("fix.md");
        mergedFile.setFilePath("fix.md");
        mergedFile.setFileSize(100L);

        when(agentPackageMapper.selectById(1L)).thenReturn(ap);
        when(packageContributionMapper.selectById(10L)).thenReturn(pc);
        when(packageVersionMapper.selectById(5L)).thenReturn(source);
        when(packageVersionMapper.selectMaxVersionNum(1L)).thenReturn(1);
        when(contributionFileMapper.selectByContributionId(10L)).thenReturn(List.of(cf));
        when(packageStorageService.saveFilesFromDirectory(anyLong(), anyString()))
                .thenReturn(List.of(mergedFile));
        when(packageStorageService.createVersionDirectory(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(java.nio.file.Files.createTempDirectory("version_").toString());

        doAnswer((Answer<Void>) invocation -> {
            PackageVersion pv = invocation.getArgument(0);
            pv.setId(99L);
            return null;
        }).when(packageVersionMapper).insert(any(PackageVersion.class));

        ContributionReviewRequest req = new ContributionReviewRequest();
        req.setStatus("approved");
        req.setReviewComment("Good work!");

        ContributionResponse response = service.review(1L, 10L, 1L, req);

        assertEquals("merged", response.getStatus());
        verify(packageContributionMapper).update(any(PackageContribution.class));
        verify(agentPackageMapper).updateCurrentVersion(eq(1L), anyLong());
    }

    // ==================== review (reject) ====================

    @Test
    void review_shouldRejectAndCleanup() {
        AgentPackage ap = createPublicPackage(1L, 1L, 10L);

        PackageContribution pc = new PackageContribution();
        pc.setId(10L);
        pc.setPackageId(1L);
        pc.setSourceVersionId(5L);
        pc.setStatus("pending");

        when(agentPackageMapper.selectById(1L)).thenReturn(ap);
        when(packageContributionMapper.selectById(10L)).thenReturn(pc);

        ContributionReviewRequest req = new ContributionReviewRequest();
        req.setStatus("rejected");
        req.setReviewComment("Not needed");

        ContributionResponse response = service.review(1L, 10L, 1L, req);

        assertEquals("rejected", response.getStatus());
        assertEquals("Not needed", response.getReviewComment());
        verify(packageStorageService).deleteDirectory(tempRoot.toString() + "/packages//contributions/tmp_10");
    }

    // ==================== review (not owner) ====================

    @Test
    void review_shouldThrow_whenNotOwner() {
        AgentPackage ap = createPublicPackage(1L, 1L, 10L);
        when(agentPackageMapper.selectById(1L)).thenReturn(ap);

        ContributionReviewRequest req = new ContributionReviewRequest();
        req.setStatus("approved");

        assertThrows(BusinessException.class, () -> service.review(1L, 10L, 2L, req));
    }
}
