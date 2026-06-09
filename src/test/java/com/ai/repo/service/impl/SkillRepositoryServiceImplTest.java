package com.ai.repo.service.impl;

import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.RepositoryNotFoundException;
import com.ai.repo.mapper.SkillRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillRepositoryServiceImplTest {

    @Mock
    private SkillRepositoryMapper skillRepositoryMapper;

    private SkillRepositoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SkillRepositoryServiceImpl();
        ReflectionTestUtils.setField(service, "skillRepositoryMapper", skillRepositoryMapper);
        ReflectionTestUtils.setField(service, "gitRootPath", "/data/git_repos/");
    }

    private SkillRepository createSampleRepo(Long id, Long agentId, String skillName) {
        SkillRepository r = new SkillRepository();
        r.setId(id);
        r.setAgentId(agentId);
        r.setUserId(1L);
        r.setSkillName(skillName);
        r.setRepoPath("/data/git_repos/agent_" + agentId + "/" + skillName + ".git");
        r.setIsPublic(false);
        return r;
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturn_whenExists() {
        SkillRepository repo = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(repo);

        SkillRepository result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(skillRepositoryMapper).selectById(1L);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(skillRepositoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(RepositoryNotFoundException.class, () -> service.findById(999L));
    }

    // ==================== findByAgentId ====================

    @Test
    void findByAgentId_shouldReturnList() {
        when(skillRepositoryMapper.selectByAgentId(10L))
                .thenReturn(List.of(createSampleRepo(1L, 10L, "weather")));

        List<SkillRepository> result = service.findByAgentId(10L);

        assertEquals(1, result.size());
        verify(skillRepositoryMapper).selectByAgentId(10L);
    }

    // ==================== create ====================

    @Test
    void create_shouldSetDefaultsAndInsert() {
        SkillRepository repo = createSampleRepo(null, 10L, "weather");
        repo.setIsPublic(null);
        repo.setEnabled(null);
        repo.setDownloadCount(null);
        repo.setLikeCount(null);

        service.create(repo);

        assertFalse(repo.getIsPublic());
        assertTrue(repo.getEnabled());
        assertEquals(0, repo.getDownloadCount());
        assertEquals(0, repo.getLikeCount());
        assertNotNull(repo.getCreatedAt());
        verify(skillRepositoryMapper).insert(repo);
    }

    // ==================== updateMetadata ====================

    @Test
    void updateMetadata_shouldUpdate_whenOwner() {
        SkillRepository existing = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(existing);

        SkillRepository updates = new SkillRepository();
        updates.setId(1L);
        updates.setAgentId(10L);
        updates.setVersion("2.0");
        updates.setDescription("Updated desc");

        when(skillRepositoryMapper.selectById(1L)).thenReturn(existing, updates);

        SkillRepository result = service.updateMetadata(updates);

        verify(skillRepositoryMapper).updateMetadata(updates);
    }

    @Test
    void updateMetadata_shouldThrow_whenNotOwner() {
        SkillRepository existing = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(existing);

        SkillRepository updates = new SkillRepository();
        updates.setId(1L);
        updates.setAgentId(99L);

        assertThrows(BusinessException.class, () -> service.updateMetadata(updates));
        verify(skillRepositoryMapper, never()).updateMetadata(any());
    }

    // ==================== delete ====================

    @Test
    void delete_shouldDeleteFromDb() {
        SkillRepository repo = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(repo);
        when(skillRepositoryMapper.deleteById(1L)).thenReturn(1);

        service.delete(1L);

        verify(skillRepositoryMapper).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(skillRepositoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(RepositoryNotFoundException.class, () -> service.delete(999L));
        verify(skillRepositoryMapper, never()).deleteById(any());
    }

    // ==================== forkRepository ====================

    @Test
    void forkRepository_shouldThrow_whenSourceNotFound() {
        when(skillRepositoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(RepositoryNotFoundException.class,
                () -> service.forkRepository(20L, 1L, 999L));
        verify(skillRepositoryMapper, never()).insert(any());
    }

    @Test
    void forkRepository_shouldThrow_whenAlreadyForked() {
        SkillRepository source = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(source);
        SkillRepository existing = createSampleRepo(2L, 20L, "weather_fork");
        when(skillRepositoryMapper.selectByAgentIdAndSkillName(20L, "weather_fork")).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.forkRepository(20L, 1L, 1L));
        assertTrue(ex.getMessage().contains("already have a fork"));
        verify(skillRepositoryMapper, never()).insert(any());
    }

    // ==================== setVisibility ====================

    @Test
    void setVisibility_shouldUpdate_whenOwner() {
        SkillRepository repo = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(repo);

        service.setVisibility(1L, 10L, true);

        verify(skillRepositoryMapper).updateVisibility(1L, true);
    }

    @Test
    void setVisibility_shouldThrow_whenNotOwner() {
        SkillRepository repo = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(repo);

        assertThrows(BusinessException.class, () -> service.setVisibility(1L, 99L, true));
        verify(skillRepositoryMapper, never()).updateVisibility(anyLong(), anyBoolean());
    }

    // ==================== incrementDownloadCount ====================

    @Test
    void incrementDownloadCount_shouldSucceed() {
        SkillRepository repo = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(repo);

        service.incrementDownloadCount(1L);

        verify(skillRepositoryMapper).incrementDownloadCount(1L);
    }

    @Test
    void incrementDownloadCount_shouldThrow_whenNotFound() {
        when(skillRepositoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(RepositoryNotFoundException.class,
                () -> service.incrementDownloadCount(999L));
        verify(skillRepositoryMapper, never()).incrementDownloadCount(any());
    }

    // ==================== incrementLikeCount ====================

    @Test
    void incrementLikeCount_shouldSucceed() {
        SkillRepository repo = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectById(1L)).thenReturn(repo);

        service.incrementLikeCount(1L);

        verify(skillRepositoryMapper).incrementLikeCount(1L);
    }

    @Test
    void incrementLikeCount_shouldThrow_whenNotFound() {
        when(skillRepositoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(RepositoryNotFoundException.class,
                () -> service.incrementLikeCount(999L));
        verify(skillRepositoryMapper, never()).incrementLikeCount(any());
    }

    // ==================== queries ====================

    @Test
    void findPublicRepos_shouldReturnList() {
        when(skillRepositoryMapper.selectPublic())
                .thenReturn(List.of(createSampleRepo(1L, 10L, "weather")));

        var result = service.findPublicRepos();

        assertEquals(1, result.size());
        verify(skillRepositoryMapper).selectPublic();
    }

    @Test
    void findByCategory_shouldReturnList() {
        when(skillRepositoryMapper.selectByCategory("java"))
                .thenReturn(List.of(createSampleRepo(1L, 10L, "weather")));

        var result = service.findByCategory("java");

        assertEquals(1, result.size());
        verify(skillRepositoryMapper).selectByCategory("java");
    }

    @Test
    void findByType_shouldReturnList() {
        when(skillRepositoryMapper.selectByType("tool"))
                .thenReturn(List.of(createSampleRepo(1L, 10L, "weather")));

        var result = service.findByType("tool");

        assertEquals(1, result.size());
        verify(skillRepositoryMapper).selectByType("tool");
    }

    @Test
    void searchByKeyword_shouldReturnList() {
        when(skillRepositoryMapper.searchByKeyword("weather"))
                .thenReturn(List.of(createSampleRepo(1L, 10L, "weather")));

        var result = service.searchByKeyword("weather");

        assertEquals(1, result.size());
        verify(skillRepositoryMapper).searchByKeyword("weather");
    }

    @Test
    void findByRepoPath_shouldReturn() {
        SkillRepository repo = createSampleRepo(1L, 10L, "weather");
        when(skillRepositoryMapper.selectByRepoPath(repo.getRepoPath())).thenReturn(repo);

        SkillRepository result = service.findByRepoPath(repo.getRepoPath());

        assertNotNull(result);
        verify(skillRepositoryMapper).selectByRepoPath(repo.getRepoPath());
    }

    @Test
    void findForksByParentId_shouldReturnList() {
        when(skillRepositoryMapper.selectByParentId(1L))
                .thenReturn(List.of(createSampleRepo(2L, 20L, "weather_fork")));

        var result = service.findForksByParentId(1L);

        assertEquals(1, result.size());
        verify(skillRepositoryMapper).selectByParentId(1L);
    }

    // ==================== sanitizePath ====================

    @Test
    void sanitizePath_shouldAcceptValidPath() {
        assertEquals("a/b/c.txt", SkillRepositoryServiceImpl.sanitizePath("a/b/c.txt"));
        assertEquals("file.json", SkillRepositoryServiceImpl.sanitizePath("file.json"));
        assertEquals("a/b", SkillRepositoryServiceImpl.sanitizePath("a\\b"));
    }

    @Test
    void sanitizePath_shouldRejectAbsolutePath() {
        assertThrows(BusinessException.class,
                () -> SkillRepositoryServiceImpl.sanitizePath("/etc/passwd"));
    }

    @Test
    void sanitizePath_shouldRejectTraversal() {
        assertThrows(BusinessException.class,
                () -> SkillRepositoryServiceImpl.sanitizePath("../../secret"));
    }

    @Test
    void sanitizePath_shouldRejectNullByte() {
        assertThrows(BusinessException.class,
                () -> SkillRepositoryServiceImpl.sanitizePath("file.txt\0.txt"));
    }

    @Test
    void sanitizePath_shouldRejectEmpty() {
        assertThrows(BusinessException.class,
                () -> SkillRepositoryServiceImpl.sanitizePath(""));
        assertThrows(BusinessException.class,
                () -> SkillRepositoryServiceImpl.sanitizePath("  "));
        assertThrows(BusinessException.class,
                () -> SkillRepositoryServiceImpl.sanitizePath(null));
    }
}
