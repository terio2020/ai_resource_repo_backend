package com.ai.repo.service;

import com.ai.repo.entity.BugReport;

import java.util.List;

public interface BugReportService {
    BugReport create(BugReport bugReport);
    BugReport update(BugReport bugReport);
    boolean updateStatus(Long id, String status);
    boolean delete(Long id);
    BugReport findById(Long id);
    BugReport findByUid(String uid);
    List<BugReport> findAll();
    List<BugReport> findByAgentId(Long agentId);
    List<BugReport> findWithFilters(Long agentId, String severity, String status, String category);
}