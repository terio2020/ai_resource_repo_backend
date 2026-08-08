package com.ai.repo.service;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AdminUserResponse;

public interface AdminUserService {

    PageResult<AdminUserResponse> listUsers(Integer page, Integer size, String keyword, String role, String status);

    void updateRole(Long operatorId, Long targetId, String role);

    void updateStatus(Long operatorId, Long targetId, String status);
}
