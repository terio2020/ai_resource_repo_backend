package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.entity.BugReport;
import com.ai.repo.entity.Memory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentPackageMapper;
import com.ai.repo.mapper.BugReportMapper;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.SkillRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminContentServiceImplTest {

    @Mock
    private MemoryMapper memoryMapper;

    @Mock
    private SkillRepositoryMapper skillRepositoryMapper;

    @Mock
    private AgentPackageMapper agentPackageMapper;

    @Mock
    private BugReportMapper bugReportMapper;

    private AdminContentServiceImpl adminContentService;

    @BeforeEach
    void setUp() throws Exception {
        adminContentService = new AdminContentServiceImpl();
        setField("memoryMapper", memoryMapper);
        setField("skillRepositoryMapper", skillRepositoryMapper);
        setField("agentPackageMapper", agentPackageMapper);
        setField("bugReportMapper", bugReportMapper);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = AdminContentServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(adminContentService, value);
    }

    @Test
    void updateMemoryStatus_shouldUpdateWhenExists() {
        Memory memory = new Memory();
        memory.setId(1L);
        when(memoryMapper.selectById(1L)).thenReturn(memory);

        adminContentService.updateMemoryStatus(7L, 1L, "BANNED");

        verify(memoryMapper).updateStatus(1L, "BANNED");
    }

    @Test
    void listMemories_shouldReturnFilteredPage() {
        Memory memory = new Memory();
        memory.setId(1L);
        when(memoryMapper.adminSelectPage("risk\\%", "BANNED", 20, 20))
                .thenReturn(Collections.singletonList(memory));
        when(memoryMapper.adminCount("risk\\%", "BANNED")).thenReturn(21L);

        PageResult<Memory> result = adminContentService.listMemories(2, 20, "risk%", "BANNED");

        assertEquals(1, result.getRecords().size());
        assertEquals(21L, result.getTotal());
        assertEquals(2L, result.getPages());
    }

    @Test
    void updateMemoryStatus_shouldThrowWhenNotFound() {
        when(memoryMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminContentService.updateMemoryStatus(7L, 1L, "BANNED"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateSkillRepoStatus_shouldUpdateWhenExists() {
        when(skillRepositoryMapper.selectById(2L)).thenReturn(new com.ai.repo.entity.SkillRepository());

        adminContentService.updateSkillRepoStatus(7L, 2L, "VISIBLE");

        verify(skillRepositoryMapper).updateStatus(2L, "VISIBLE");
    }

    @Test
    void updatePackageStatus_shouldUpdateWhenExists() {
        when(agentPackageMapper.selectById(3L)).thenReturn(new com.ai.repo.entity.AgentPackage());

        adminContentService.updatePackageStatus(7L, 3L, "BANNED");

        verify(agentPackageMapper).updateStatus(3L, "BANNED");
    }

    @Test
    void updatePackageStatus_shouldThrowWhenNotFound() {
        when(agentPackageMapper.selectById(3L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> adminContentService.updatePackageStatus(7L, 3L, "BANNED"));
    }

    @Test
    void listBugReports_shouldReturnPage() {
        BugReport report = new BugReport();
        report.setId(1L);
        when(bugReportMapper.selectAdminPage("open", "high", 10, 20)).thenReturn(Collections.singletonList(report));
        when(bugReportMapper.adminCount("open", "high")).thenReturn(42L);

        PageResult<BugReport> result = adminContentService.listBugReports(3, 10, "open", "high");

        assertEquals(1, result.getRecords().size());
        assertEquals(42L, result.getTotal());
        assertEquals(3L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(5L, result.getPages());
        verify(bugReportMapper).selectAdminPage("open", "high", 10, 20);
    }

    @Test
    void getBugReport_shouldReturnExistingReport() {
        BugReport report = new BugReport();
        report.setId(9L);
        when(bugReportMapper.selectById(9L)).thenReturn(report);

        assertEquals(report, adminContentService.getBugReport(9L));
    }

    @Test
    void getBugReport_shouldRejectMissingReport() {
        when(bugReportMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminContentService.getBugReport(9L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void listBugReports_shouldApplyDefaultsForInvalidPaging() {
        when(bugReportMapper.selectAdminPage(null, null, 10, 0)).thenReturn(Collections.emptyList());
        when(bugReportMapper.adminCount(null, null)).thenReturn(0L);

        adminContentService.listBugReports(0, -5, null, null);

        verify(bugReportMapper).selectAdminPage(null, null, 10, 0);
    }

    @Test
    void updateBugReportStatus_shouldUpdateWhenExists() {
        when(bugReportMapper.selectById(4L)).thenReturn(new BugReport());

        adminContentService.updateBugReportStatus(7L, 4L, "resolved");

        verify(bugReportMapper).updateStatus(4L, "resolved");
    }

    @Test
    void updateBugReportStatus_shouldThrowWhenNotFound() {
        when(bugReportMapper.selectById(4L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminContentService.updateBugReportStatus(7L, 4L, "resolved"));
        assertEquals(404, ex.getCode());
    }
}
