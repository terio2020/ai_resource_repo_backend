package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.entity.BugReport;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.entity.AgentPackage;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentPackageMapper;
import com.ai.repo.mapper.BugReportMapper;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.SkillRepositoryMapper;
import com.ai.repo.service.AdminContentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AdminContentServiceImpl implements AdminContentService {

    @Resource
    private MemoryMapper memoryMapper;

    @Resource
    private SkillRepositoryMapper skillRepositoryMapper;

    @Resource
    private AgentPackageMapper agentPackageMapper;

    @Resource
    private BugReportMapper bugReportMapper;

    @Override
    public PageResult<Memory> listMemories(Integer page, Integer size, String keyword, String status) {
        int[] paging = paging(page, size);
        String escaped = escapeLike(keyword);
        return new PageResult<>(memoryMapper.adminSelectPage(escaped, status, paging[1], paging[2]),
                memoryMapper.adminCount(escaped, status), (long) paging[0], (long) paging[1]);
    }

    @Override
    public PageResult<SkillRepository> listSkillRepos(Integer page, Integer size, String keyword, String status) {
        int[] paging = paging(page, size);
        String escaped = escapeLike(keyword);
        return new PageResult<>(skillRepositoryMapper.adminSelectPage(escaped, status, paging[1], paging[2]),
                skillRepositoryMapper.adminCount(escaped, status), (long) paging[0], (long) paging[1]);
    }

    @Override
    public PageResult<AgentPackage> listPackages(Integer page, Integer size, String keyword, String status) {
        int[] paging = paging(page, size);
        String escaped = escapeLike(keyword);
        return new PageResult<>(agentPackageMapper.adminSelectPage(escaped, status, paging[1], paging[2]),
                agentPackageMapper.adminCount(escaped, status), (long) paging[0], (long) paging[1]);
    }

    @Override
    public void updateMemoryStatus(Long operatorId, Long memoryId, String status) {
        if (memoryMapper.selectById(memoryId) == null) {
            throw new BusinessException(404, "Memory not found");
        }
        memoryMapper.updateStatus(memoryId, status);
        log.warn("[AUDIT] admin={} action=updateMemoryStatus targetId={} value={}", operatorId, memoryId, status);
    }

    @Override
    public void updateSkillRepoStatus(Long operatorId, Long repoId, String status) {
        if (skillRepositoryMapper.selectById(repoId) == null) {
            throw new BusinessException(404, "Skill repository not found");
        }
        skillRepositoryMapper.updateStatus(repoId, status);
        log.warn("[AUDIT] admin={} action=updateSkillRepoStatus targetId={} value={}", operatorId, repoId, status);
    }

    @Override
    public void updatePackageStatus(Long operatorId, Long packageId, String status) {
        if (agentPackageMapper.selectById(packageId) == null) {
            throw new BusinessException(404, "Package not found");
        }
        agentPackageMapper.updateStatus(packageId, status);
        log.warn("[AUDIT] admin={} action=updatePackageStatus targetId={} value={}", operatorId, packageId, status);
    }

    @Override
    public PageResult<BugReport> listBugReports(Integer page, Integer size, String status, String severity) {
        int actualPage = page != null && page > 0 ? page : 1;
        int actualSize = size != null && size > 0 ? size : 10;
        int offset = (actualPage - 1) * actualSize;

        List<BugReport> records = bugReportMapper.selectAdminPage(status, severity, actualSize, offset);
        Long total = bugReportMapper.adminCount(status, severity);
        return new PageResult<>(records, total, (long) actualPage, (long) actualSize);
    }

    @Override
    public BugReport getBugReport(Long bugReportId) {
        BugReport report = bugReportMapper.selectById(bugReportId);
        if (report == null) {
            throw new BusinessException(404, "Bug report not found");
        }
        return report;
    }

    @Override
    public void updateBugReportStatus(Long operatorId, Long bugReportId, String status) {
        if (bugReportMapper.selectById(bugReportId) == null) {
            throw new BusinessException(404, "Bug report not found");
        }
        bugReportMapper.updateStatus(bugReportId, status);
        log.warn("[AUDIT] admin={} action=updateBugReportStatus targetId={} value={}", operatorId, bugReportId, status);
    }

    private int[] paging(Integer page, Integer size) {
        int actualPage = page != null && page > 0 ? page : 1;
        int actualSize = size != null && size > 0 ? size : 10;
        return new int[]{actualPage, actualSize, (actualPage - 1) * actualSize};
    }

    private String escapeLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return keyword;
        }
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
