package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.common.Result;
import com.ai.repo.dto.*;
import com.ai.repo.entity.User;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.TempTokenService;
import com.ai.repo.service.UserService;
import com.ai.repo.util.PasswordEncoderUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "User API", description = "User authentication and management operations")
public class UserController {

    @jakarta.annotation.Resource
    UserService userService;

    @jakarta.annotation.Resource
    PasswordEncoderUtil passwordEncoderUtil;

    @jakarta.annotation.Resource
    TempTokenService tempTokenService;

    @jakarta.annotation.Resource
    com.ai.repo.jwt.JwtProvider jwtProvider;

    @PostMapping
    @Operation(summary = "Create a new user", description = "Register a new user with provided credentials")
    public ResponseEntity<Result<Void>> createUser(@RequestBody UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoderUtil.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userService.create(user);
        return Result.ok();
    }

    @PostMapping("/update")
    @RequireAuth
    @Operation(summary = "Update user", description = "Update current user's information (partial update)")
    public ResponseEntity<Result<User>> updateUser(
            HttpServletRequest request,
            @Valid @RequestBody UserUpdateRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "Unauthorized");
        }

        User user = new User();
        user.setId(userId);
        if (req.getUsername() != null) {
            user.setUsername(req.getUsername());
        }
        if (req.getPassword() != null) {
            user.setPassword(req.getPassword());
        }
        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatar() != null) {
            user.setAvatar(req.getAvatar());
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getXHandle() != null) {
            user.setXHandle(req.getXHandle());
        }
        if (req.getXName() != null) {
            user.setXName(req.getXName());
        }
        if (req.getXAvatar() != null) {
            user.setXAvatar(req.getXAvatar());
        }

        User updatedUser = userService.update(user);
        updatedUser.setHasPassword(updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty());
        updatedUser.setPassword(null);
        updatedUser.setAccessToken(null);
        updatedUser.setRefreshToken(null);
        return Result.ok(updatedUser);
    }

    @PostMapping("/deleteById")
    @RequireAuth
    @RequireOwnership(resourceType = "user", idParam = "id")
    @Operation(summary = "Delete user", description = "Delete a user account (owner-only)")
    public ResponseEntity<Result<Void>> deleteUser(@Parameter(description = "User ID") @RequestParam @Min(1) Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and generate JWT tokens")
    public ResponseEntity<Result<LoginResponse>> login(@RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            return Result.fail(401, "Invalid username or password");
        }

        if (!userService.verifyPassword(request.getUsername(), request.getPassword())) {
            return Result.fail(401, "Invalid username or password");
        }

        userService.updateLoginTime(user.getId());
        LoginResponse response = userService.generateTokens(user.getId());

        return Result.ok(response);
    }

    @PostMapping("/login/email")
    @Operation(summary = "User login by email", description = "Authenticate user by email and password, generate JWT tokens")
    public ResponseEntity<Result<LoginResponse>> loginByEmail(@RequestBody EmailLoginRequest request) {
        User user = userService.findByEmail(request.getEmail());
        if (user == null) {
            return Result.fail(401, "Invalid email or password");
        }

        if (!userService.verifyPasswordByEmail(request.getEmail(), request.getPassword())) {
            return Result.fail(401, "Invalid email or password");
        }

        userService.updateLoginTime(user.getId());
        LoginResponse response = userService.generateTokens(user.getId());

        return Result.ok(response);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Refresh JWT access token using refresh token")
    public ResponseEntity<Result<TokenRefreshResponse>> refreshToken(@RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = userService.refreshToken(request.getRefreshToken());
        return Result.ok(response);
    }

    @PostMapping("/logout")
    @RequireAuth
    @Operation(summary = "User logout", description = "Logout user and invalidate tokens")
    public ResponseEntity<Result<Void>> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            userService.clearTokens(userId);
        }
        return Result.ok();
    }

    @PostMapping("/auth-login")
    @Operation(summary = "Agent login with session", description = "Login and store accessToken for Agent authentication flow")
    public ResponseEntity<Result<Void>> authLogin(@RequestBody LoginRequest request, @RequestParam("sessionId") String sessionId) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            return Result.fail(401, "Invalid username or password");
        }

        if (!userService.verifyPassword(request.getUsername(), request.getPassword())) {
            return Result.fail(401, "Invalid username or password");
        }

        userService.updateLoginTime(user.getId());
        LoginResponse response = userService.generateTokens(user.getId());

        tempTokenService.storeToken(sessionId, response.getAccessToken());

        return Result.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Return current user's non-sensitive information based on token")
    public ResponseEntity<Result<User>> getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.fail(401, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Long userId = jwtProvider.validateAccessToken(token);

        if (userId == null) {
            return Result.fail(401, "Invalid or expired token");
        }

        User user = userService.findById(userId);
        if (user == null) {
            return Result.fail(404, "User not found");
        }

        user.setHasPassword(user.getPassword() != null && !user.getPassword().isEmpty());
        user.setPassword(null);
        user.setAccessToken(null);
        user.setRefreshToken(null);

        return Result.ok(user);
    }
}
