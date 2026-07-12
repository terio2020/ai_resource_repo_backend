package com.ai.repo.service.impl;

import com.ai.repo.entity.BugReport;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.BugReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BugReportServiceImplTest {

    @Mock
    private BugReportMapper bugReportMapper;

    private BugReportServiceImpl bugReportService;

    @BeforeEach
    void setUp() throws Exception {
        bugReportService = new BugReportServiceImpl();
        java.lang.reflect.Field mapperField = BugReportServiceImpl.class.getDeclaredField("bugReportMapper");
        mapperField.setAccessible(true);
        mapperField.set(bugReportService, bugReportMapper);
    }

    private BugReport buildBugReport(Long id, Long agentId, String title) {
        BugReport bug = new BugReport();
        bug.setId(id);
        bug.setAgentId(agentId);
        bug.setTitle(title);
        bug.setDescription("Test description");
        bug.setSeverity("error");
        bug.setStatus("open");
        bug.setCreatedAt(LocalDateTime.now());
        bug.setUpdatedAt(LocalDateTime.now());
        return bug;
    }

    // ==================== create ====================

    @Test
    void create_shouldInsertAndReturnBugReport() {
        BugReport bug = buildBugReport(null, 5L, "Test bug");
        bug.setUid(null);
        when(bugReportMapper.insert(any(BugReport.class))).thenReturn(1);

        BugReport result = bugReportService.create(bug);

        assertNotNull(result);
        assertNotNull(result.getUid());
        verify(bugReportMapper).insert(bug);
    }

    @Test
    void create_withExistingUid_shouldNotOverwrite() {
        BugReport bug = buildBugReport(null, 5L, "Test bug");
        bug.setUid("existing-uid");
        when(bugReportMapper.insert(any(BugReport.class))).thenReturn(1);

        BugReport result = bugReportService.create(bug);

        assertEquals("existing-uid", result.getUid());
        verify(bugReportMapper).insert(bug);
    }

    // ==================== update ====================

    @Test
    void update_shouldUpdateExistingBugReport() {
        BugReport bug = buildBugReport(1L, 5L, "Updated bug");
        when(bugReportMapper.selectById(1L)).thenReturn(bug);
        when(bugReportMapper.update(any(BugReport.class))).thenReturn(1);

        BugReport result = bugReportService.update(bug);

        assertEquals("Updated bug", result.getTitle());
        verify(bugReportMapper).update(bug);
    }

    @Test
    void update_notFound_shouldThrowException() {
        BugReport bug = buildBugReport(999L, 5L, "Not found");
        when(bugReportMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> bugReportService.update(bug));
        verify(bugReportMapper, never()).update(any());
    }

    // ==================== updateStatus ====================

    @Test
    void updateStatus_shouldUpdateStatus() {
        when(bugReportMapper.updateStatus(1L, "resolved")).thenReturn(1);

        boolean result = bugReportService.updateStatus(1L, "resolved");

        assertTrue(result);
        verify(bugReportMapper).updateStatus(1L, "resolved");
    }

    @Test
    void updateStatus_notFound_shouldThrowException() {
        when(bugReportMapper.updateStatus(999L, "resolved")).thenReturn(0);

        assertThrows(BusinessException.class, () -> bugReportService.updateStatus(999L, "resolved"));
    }

    // ==================== delete ====================

    @Test
    void delete_shouldDeleteExistingBugReport() {
        BugReport bug = buildBugReport(1L, 5L, "Delete me");
        when(bugReportMapper.selectById(1L)).thenReturn(bug);
        when(bugReportMapper.deleteById(1L)).thenReturn(1);

        boolean result = bugReportService.delete(1L);

        assertTrue(result);
        verify(bugReportMapper).deleteById(1L);
    }

    @Test
    void delete_notFound_shouldThrowException() {
        when(bugReportMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> bugReportService.delete(999L));
        verify(bugReportMapper, never()).deleteById(anyLong());
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturnBugReport() {
        BugReport bug = buildBugReport(1L, 5L, "Test bug");
        when(bugReportMapper.selectById(1L)).thenReturn(bug);

        BugReport result = bugReportService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test bug", result.getTitle());
    }

    @Test
    void findById_notFound_shouldReturnNull() {
        when(bugReportMapper.selectById(999L)).thenReturn(null);

        BugReport result = bugReportService.findById(999L);

        assertNull(result);
    }

    // ==================== findByUid ====================

    @Test
    void findByUid_shouldReturnBugReport() {
        BugReport bug = buildBugReport(1L, 5L, "Test bug");
        bug.setUid("abc123");
        when(bugReportMapper.selectByUid("abc123")).thenReturn(bug);

        BugReport result = bugReportService.findByUid("abc123");

        assertNotNull(result);
        assertEquals("abc123", result.getUid());
    }

    // ==================== findAll ====================

    @Test
    void findAll_shouldReturnList() {
        List<BugReport> bugs = Arrays.asList(
                buildBugReport(1L, 5L, "Bug 1"),
                buildBugReport(2L, 5L, "Bug 2")
        );
        when(bugReportMapper.selectAll()).thenReturn(bugs);

        List<BugReport> result = bugReportService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findAll_empty_shouldReturnEmptyList() {
        when(bugReportMapper.selectAll()).thenReturn(Arrays.asList());

        List<BugReport> result = bugReportService.findAll();

        assertTrue(result.isEmpty());
    }

    // ==================== findByAgentId ====================

    @Test
    void findByAgentId_shouldReturnBugReports() {
        List<BugReport> bugs = Arrays.asList(
                buildBugReport(1L, 5L, "Bug 1"),
                buildBugReport(2L, 5L, "Bug 2")
        );
        when(bugReportMapper.selectByAgentId(5L)).thenReturn(bugs);

        List<BugReport> result = bugReportService.findByAgentId(5L);

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).getAgentId());
    }

    // ==================== findWithFilters ====================

    @Test
    void findWithFilters_allFilters_shouldReturnFiltered() {
        List<BugReport> bugs = Arrays.asList(buildBugReport(1L, 5L, "Critical bug"));
        when(bugReportMapper.selectWithFilters(5L, "critical", "open", "api")).thenReturn(bugs);

        List<BugReport> result = bugReportService.findWithFilters(5L, "critical", "open", "api");

        assertEquals(1, result.size());
        verify(bugReportMapper).selectWithFilters(5L, "critical", "open", "api");
    }

    @Test
    void findWithFilters_noFilters_shouldReturnAll() {
        List<BugReport> bugs = Arrays.asList(
                buildBugReport(1L, 5L, "Bug 1"),
                buildBugReport(2L, 6L, "Bug 2")
        );
        when(bugReportMapper.selectWithFilters(null, null, null, null)).thenReturn(bugs);

        List<BugReport> result = bugReportService.findWithFilters(null, null, null, null);

        assertEquals(2, result.size());
    }
}