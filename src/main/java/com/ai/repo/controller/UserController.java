package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.*;
import com.ai.repo.entity.User;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.TempTokenService;
import com.ai.repo.service.UserService;
import com.ai.repo.util.PasswordEncoderUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User API", description = "User authentication and management operations")
public class UserController {

    @Resource
    UserService userService;

    @Resource
    PasswordEncoderUtil passwordEncoderUtil;

    @Resource
    TempTokenService tempTokenService;

    @Resource
    com.ai.repo.jwt.JwtProvider jwtProvider;

    @PostMapping
    @Operation(summary = "Create a new user", description = "Register a new user with provided credentials")
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
    @Operation(summary = "Update user", description = "Update user information")
    public Result<User> updateUser(
            @Parameter(description = "User ID") @RequestParam Long id,
            @RequestBody User user) {
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoderUtil.encode(user.getPassword()));
        }
        User updatedUser = userService.update(user);
        return Result.success(updatedUser);
    }

    @PostMapping("/deleteById")
    @RequireAuth
    @Operation(summary = "Delete user", description = "Delete a user account")
    public Result<Void> deleteUser(@Parameter(description = "User ID") @RequestParam Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and generate JWT tokens")
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
    @Operation(summary = "Refresh access token", description = "Refresh JWT access token using refresh token")
    public Result<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = userService.refreshToken(request.getRefreshToken());
        return Result.success(response);
    }

    @PostMapping("/logout")
    @RequireAuth
    @Operation(summary = "User logout", description = "Logout user and invalidate tokens")
    public Result<Void> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            userService.clearTokens(userId);
        }
        return Result.success();
    }

    @PostMapping("/auth-login")
    @Operation(summary = "Agent login with session", description = "Login and store accessToken for Agent authentication flow")
    public Result<Void> authLogin(@RequestBody LoginRequest request, @RequestParam("sessionId") String sessionId) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            return Result.error(401, "Invalid username or password");
        }

        if (!userService.verifyPassword(request.getUsername(), request.getPassword())) {
            return Result.error(401, "Invalid username or password");
        }

        userService.updateLoginTime(user.getId());
        LoginResponse response = userService.generateTokens(user.getId());

        tempTokenService.storeToken(sessionId, response.getAccessToken());

        return Result.success();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Return current user's non-sensitive information based on token")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error(401, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Long userId = jwtProvider.validateAccessToken(token);

        if (userId == null) {
            return Result.error(401, "Invalid or expired token");
        }

        User user = userService.findById(userId);
        if (user == null) {
            return Result.error(404, "User not found");
        }

        user.setPassword(null);
        user.setAccessToken(null);
        user.setRefreshToken(null);

        return Result.success(user);
    }
}
