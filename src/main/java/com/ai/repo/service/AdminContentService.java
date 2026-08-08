package com.ai.repo.service;

import com.ai.repo.common.PageResult;
import com.ai.repo.entity.BugReport;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.entity.AgentPackage;

public interface AdminContentService {

    PageResult<Memory> listMemories(Integer page, Integer size, String keyword, String status);

    PageResult<SkillRepository> listSkillRepos(Integer page, Integer size, String keyword, String status);

    PageResult<AgentPackage> listPackages(Integer page, Integer size, String keyword, String status);

    void updateMemoryStatus(Long operatorId, Long memoryId, String status);

    void updateSkillRepoStatus(Long operatorId, Long repoId, String status);

    void updatePackageStatus(Long operatorId, Long packageId, String status);

    PageResult<BugReport> listBugReports(Integer page, Integer size, String status, String severity);

    BugReport getBugReport(Long bugReportId);

    void updateBugReportStatus(Long operatorId, Long bugReportId, String status);
}
