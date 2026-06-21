package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@Validated
@Tag(name = "Avatar API", description = "User avatar management operations")
public class AvatarController {

    @jakarta.annotation.Resource
    private UserService userService;

    @Value("${file.storage.base-path:/data/logicoma-files}")
    private String basePath;

    @PostMapping("/{userId}/avatar")
    @RequireAuth
    @Operation(summary = "Upload avatar image")
    public Result<Map<String, String>> uploadAvatar(
            @Parameter(description = "User ID") @PathVariable @Min(1) Long userId,
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
            boolean preserveAlpha = "png".equals(extension) || "gif".equals(extension) || "webp".equals(extension);
            String outputFormat = preserveAlpha ? "png" : "jpg";
            if ("svg".equals(extension) || "bmp".equals(extension)) {
                outputFormat = "png";
            }

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
            throw new BusinessException("Failed to upload avatar: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/avatar/{fileName}")
    @Operation(summary = "Get avatar image (legacy — kept for backward compatibility)")
    public ResponseEntity<Resource> getAvatar(
            @Parameter(description = "User ID") @PathVariable @Min(1) Long userId,
            @Parameter(description = "File name") @PathVariable String fileName) {
        try {
            Path filePath = Paths.get(basePath, "users", String.valueOf(userId), fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = getContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }

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
