package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.*;
import com.ai.repo.entity.User;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.UserService;
import com.ai.repo.util.PasswordEncoderUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    UserService userService;

    @Resource
    PasswordEncoderUtil passwordEncoderUtil;

    @PostMapping
    public Result<Void> createUser(@RequestBody UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoderUtil.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userService.create(user);
        return Result.success();
    }

    @PostMapping("/update")
    @RequireAuth
    public Result<User> updateUser(@RequestParam Long id, @RequestBody User user) {
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoderUtil.encode(user.getPassword()));
        }
        User updatedUser = userService.update(user);
        return Result.success(updatedUser);
    }

    @PostMapping("/deleteById")
    @RequireAuth
    public Result<Void> deleteUser(@RequestParam Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            return Result.error(401, "Invalid username or password");
        }
        
        if (!userService.verifyPassword(request.getUsername(), request.getPassword())) {
            return Result.error(401, "Invalid username or password");
        }
        
        userService.updateLoginTime(user.getId());
        LoginResponse response = userService.generateTokens(user.getId());
        
        return Result.success(response);
    }

    @PostMapping("/refresh-token")
    public Result<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = userService.refreshToken(request.getRefreshToken());
        return Result.success(response);
    }

    @PostMapping("/logout")
    @RequireAuth
    public Result<Void> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            userService.clearTokens(userId);
        }
        return Result.success();
    }
}
