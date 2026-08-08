package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AdminUserResponse;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.service.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private AgentMapper agentMapper;

    @Override
    public PageResult<AdminUserResponse> listUsers(Integer page, Integer size, String keyword, String role, String status) {
        int actualPage = page != null && page > 0 ? page : 1;
        int actualSize = size != null && size > 0 ? size : 10;
        int offset = (actualPage - 1) * actualSize;
        String escaped = escapeLike(keyword);

        List<User> records = userMapper.adminSelectPage(escaped, role, status, actualSize, offset);
        List<AdminUserResponse> items = records.stream().map(this::toResponse).collect(Collectors.toList());
        Long total = userMapper.adminCount(escaped, role, status);

        return new PageResult<>(items, total, (long) actualPage, (long) actualSize);
    }

    @Override
    public void updateRole(Long operatorId, Long targetId, String role) {
        if (targetId.equals(operatorId)) {
            throw new BusinessException(400, "Cannot change your own role");
        }
        User user = getUserOrThrow(targetId);
        userMapper.updateRoleAndStatus(targetId, role, user.getStatus());
        log.warn("[AUDIT] admin={} action=updateUserRole targetId={} value={}", operatorId, targetId, role);
    }

    @Override
    @Transactional
    public void updateStatus(Long operatorId, Long targetId, String status) {
        User user = getUserOrThrow(targetId);
        if ("DISABLED".equals(status)) {
            if (targetId.equals(operatorId)) {
                throw new BusinessException(400, "Cannot disable your own account");
            }
            if ("ADMIN".equals(user.getRole()) && "ACTIVE".equals(user.getStatus())) {
                Long activeAdmins = userMapper.countActiveAdmins();
                if (activeAdmins != null && activeAdmins <= 1) {
                    throw new BusinessException(400, "Cannot disable the last active admin");
                }
            }
            agentMapper.disableByUserId(targetId);
        }
        userMapper.updateRoleAndStatus(targetId, user.getRole(), status);
        log.warn("[AUDIT] admin={} action=updateUserStatus targetId={} value={}", operatorId, targetId, status);
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }

    private AdminUserResponse toResponse(User user) {
        AdminUserResponse resp = new AdminUserResponse();
        resp.setId(user.getId());
        resp.setUid(user.getUid());
        resp.setUsername(user.getUsername());
        resp.setEmail(user.getEmail());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setRole(user.getRole());
        resp.setStatus(user.getStatus());
        resp.setLastLoginAt(user.getLastLoginAt());
        resp.setCreatedAt(user.getCreatedAt());
        return resp;
    }

    private String escapeLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return keyword;
        }
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
