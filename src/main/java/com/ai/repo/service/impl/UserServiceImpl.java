package com.ai.repo.service.impl;

import com.ai.repo.dto.LoginResponse;
import com.ai.repo.dto.TokenRefreshResponse;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.jwt.JwtProvider;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.service.UserService;
import com.ai.repo.util.PasswordEncoderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoderUtil passwordEncoderUtil;

    @Autowired
    private JwtProvider jwtProvider;

    @Override
    public User create(User user) {
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("Username already exists");
        }
        if (userMapper.selectByEmail(user.getEmail()) != null) {
            throw new BusinessException("Email already exists");
        }
        if (passwordEncoderUtil.needsEncoding(user.getPassword())) {
            user.setPassword(passwordEncoderUtil.encode(user.getPassword()));
        }
        userMapper.insert(user);
        return user;
    }

    @Override
    public User update(User user) {
        if (userMapper.selectById(user.getId()) == null) {
            throw new BusinessException("User not found");
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty() && passwordEncoderUtil.needsEncoding(user.getPassword())) {
            user.setPassword(passwordEncoderUtil.encode(user.getPassword()));
        }
        userMapper.update(user);
        return user;
    }

    @Override
    public boolean delete(Long id) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException("User not found");
        }
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userMapper.selectAll();
    }

    @Override
    public List<User> findByStatus(String status) {
        return userMapper.selectByStatus(status);
    }

    @Override
    public List<User> findByRole(String role) {
        return userMapper.selectByRole(role);
    }

    @Override
    public boolean verifyPassword(String username, String rawPassword) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return false;
        }
        return passwordEncoderUtil.matches(rawPassword, user.getPassword());
    }

    @Override
    public boolean verifyPasswordByEmail(String email, String rawPassword) {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            return false;
        }
        return passwordEncoderUtil.matches(rawPassword, user.getPassword());
    }

    @Override
    public LoginResponse generateTokens(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        String accessToken = jwtProvider.generateAccessToken(userId, user.getUsername());
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusMinutes(60);

        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
        user.setTokenExpiresAt(tokenExpiresAt);
        userMapper.update(user);

        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenExpiresAt(tokenExpiresAt);
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }

    @Override
    public TokenRefreshResponse refreshToken(String refreshToken) {
        Long userId = jwtProvider.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw new BusinessException("Invalid refresh token");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId, user.getUsername());
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusMinutes(60);

        user.setAccessToken(newAccessToken);
        user.setTokenExpiresAt(tokenExpiresAt);
        userMapper.update(user);

        TokenRefreshResponse response = new TokenRefreshResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresAt(tokenExpiresAt);

        return response;
    }

    @Override
    public void clearTokens(Long userId) {
        jwtProvider.clearTokens(userId);
        
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setAccessToken(null);
            user.setRefreshToken(null);
            user.setTokenExpiresAt(null);
            userMapper.update(user);
        }
    }

    @Override
    public void updateLoginTime(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.update(user);
        }
    }
}
