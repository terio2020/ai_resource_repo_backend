package com.ai.repo.service.impl;

import com.ai.repo.entity.BugReport;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.BugReportMapper;
import com.ai.repo.service.BugReportService;
import com.ai.repo.util.UuidUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BugReportServiceImpl implements BugReportService {

    @Resource
    private BugReportMapper bugReportMapper;

    @Override
    public BugReport create(BugReport bugReport) {
        if (bugReport.getUid() == null || bugReport.getUid().isEmpty()) {
            bugReport.setUid(UuidUtil.generate());
        }
        bugReportMapper.insert(bugReport);
        return bugReport;
    }

    @Override
    public BugReport update(BugReport bugReport) {
        if (bugReportMapper.selectById(bugReport.getId()) == null) {
            throw new BusinessException("Bug report not found");
        }
        bugReportMapper.update(bugReport);
        return bugReport;
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        int rows = bugReportMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException("Bug report not found");
        }
        return true;
    }

    @Override
    public boolean delete(Long id) {
        if (bugReportMapper.selectById(id) == null) {
            throw new BusinessException("Bug report not found");
        }
        return bugReportMapper.deleteById(id) > 0;
    }

    @Override
    public BugReport findById(Long id) {
        return bugReportMapper.selectById(id);
    }

    @Override
    public BugReport findByUid(String uid) {
        return bugReportMapper.selectByUid(uid);
    }

    @Override
    public List<BugReport> findAll() {
        return bugReportMapper.selectAll();
    }

    @Override
    public List<BugReport> findByAgentId(Long agentId) {
        return bugReportMapper.selectByAgentId(agentId);
    }

    @Override
    public List<BugReport> findWithFilters(Long agentId, String severity, String status, String category) {
        return bugReportMapper.selectWithFilters(agentId, severity, status, category);
    }
}