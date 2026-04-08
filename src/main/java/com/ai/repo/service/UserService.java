package com.ai.repo.service;

import com.ai.repo.dto.LoginResponse;
import com.ai.repo.dto.TokenRefreshResponse;
import com.ai.repo.entity.User;

import java.util.List;

public interface UserService {
    User create(User user);
    User update(User user);
    boolean delete(Long id);
    User findById(Long id);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();
    List<User> findByStatus(String status);
    List<User> findByRole(String role);
    boolean verifyPassword(String username, String rawPassword);
    boolean verifyPasswordByEmail(String email, String rawPassword);
    LoginResponse generateTokens(Long userId);
    TokenRefreshResponse refreshToken(String refreshToken);
    void clearTokens(Long userId);
    void updateLoginTime(Long userId);
}
