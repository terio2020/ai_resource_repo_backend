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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
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

    @Value("${file.storage.base-path:/data/logicoma-files}")
    private String basePath;

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
    @Operation(summary = "Update user", description = "Update current user's information (partial update)")
    public Result<User> updateUser(
            HttpServletRequest request,
            @RequestBody UserUpdateRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "Unauthorized");
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
        updatedUser.setPassword(null);
        updatedUser.setAccessToken(null);
        updatedUser.setRefreshToken(null);
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

    @PostMapping("/login/email")
    @Operation(summary = "User login by email", description = "Authenticate user by email and password, generate JWT tokens")
    public Result<LoginResponse> loginByEmail(@RequestBody EmailLoginRequest request) {
        User user = userService.findByEmail(request.getEmail());
        if (user == null) {
            return Result.error(401, "Invalid email or password");
        }

        if (!userService.verifyPasswordByEmail(request.getEmail(), request.getPassword())) {
            return Result.error(401, "Invalid email or password");
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

    @PostMapping("/{userId}/avatar")
    @RequireAuth
    @Operation(summary = "Upload avatar image")
    public Result<Map<String, String>> uploadAvatar(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Avatar image file") @RequestParam("avatar") MultipartFile file,
            HttpServletRequest request) {

        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null || !currentUserId.equals(userId)) {
            return Result.error(403, "Access denied");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex + 1).toLowerCase();
            }
        }

        Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
        if (!allowedExtensions.contains(extension)) {
            return Result.error(400, "Only image files (jpg, png, gif, webp, svg, bmp) are allowed");
        }

        try {
            // Determine output format (JPEG for photos, PNG for images with transparency)
            boolean preserveAlpha = "png".equals(extension) || "gif".equals(extension) || "webp".equals(extension);
            String outputFormat = preserveAlpha ? "png" : "jpg";
            if ("svg".equals(extension) || "bmp".equals(extension)) {
                outputFormat = "png";
            }

            // Read image and compress/resize to max 200×200
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                return Result.error(400, "Unable to read image file");
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            int maxSize = 200;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (width > maxSize || height > maxSize) {
                double scale = Math.min((double) maxSize / width, (double) maxSize / height);
                int newWidth = Math.max(1, (int) (width * scale));
                int newHeight = Math.max(1, (int) (height * scale));

                BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resizedImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                g2d.dispose();
                ImageIO.write(resizedImage, outputFormat, baos);
            } else {
                // Already ≤200px — still write through ImageIO for consistent compression
                ImageIO.write(originalImage, outputFormat, baos);
            }

            String extForFile = outputFormat.equals("jpg") ? "jpg" : "png";
            String fileName = userId + "_" + System.currentTimeMillis() + "." + extForFile;
            String avatarDir = basePath + "/users/" + userId;
            Files.createDirectories(Paths.get(avatarDir));
            Files.write(Paths.get(avatarDir, fileName), baos.toByteArray());

            String avatarUrl = "/avatars/users/" + userId + "/" + fileName;
            User user = new User();
            user.setId(userId);
            user.setAvatar(avatarUrl);
            userService.update(user);

            Map<String, String> result = new HashMap<>();
            result.put("avatar", avatarUrl);
            return Result.success(result);
        } catch (IOException e) {
            throw new com.ai.repo.exception.BusinessException("Failed to upload avatar: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/avatar/{fileName}")
    @Operation(summary = "Get avatar image (legacy — kept for backward compatibility)")
    public ResponseEntity<org.springframework.core.io.Resource> getAvatar(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "File name") @PathVariable String fileName) {
        try {
            // First try the new storage path
            Path filePath = Paths.get(basePath, "users", String.valueOf(userId), fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = getContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }

            // Fallback: try old storage path for legacy avatars not yet migrated
            Path oldPath = Paths.get(basePath.replace("/avatars-data", ""), "avatars", String.valueOf(userId), fileName).normalize();
            Resource oldResource = new UrlResource(oldPath.toUri());
            if (oldResource.exists() && oldResource.isReadable()) {
                String contentType = getContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(oldResource);
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String getContentType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String ext = dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
        switch (ext) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "bmp": return "image/bmp";
            case "svg": return "image/svg+xml";
            default: return "application/octet-stream";
        }
    }
}
