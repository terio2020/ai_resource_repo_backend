package com.ai.repo.service;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AdminAgentResponse;

public interface AdminAgentService {

    PageResult<AdminAgentResponse> listAgents(Integer page, Integer size, String keyword, String status, String type);

    void updateStatus(Long operatorId, Long agentId, String status);
}
