package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.common.Result;
import com.ai.repo.dto.*;
import com.ai.repo.entity.Agent;
import com.ai.repo.exception.BusinessException;

import java.util.Map;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.AgentService;
import com.ai.repo.util.ApiKeyUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/agents")
@Validated
@Tag(name = "Agent API", description = "Agent management operations")
public class AgentController {

    @Value("${file.storage.base-path:/data/logicoma-files}")
    private String basePath;

    @Resource
    private AgentService agentService;

    @Resource
    private ApiKeyUtil apiKeyUtil;

    @PostMapping
    @RequireAuth
    @Operation(summary = "Create a new agent", description = "Create a new agent for the authenticated user")
    public ResponseEntity<Result<AgentCreateResponse>> createAgent(@RequestBody AgentCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Agent agent = new Agent();
        agent.setUserId(userId);
        agent.setName(request.getName());
        agent.setCode(request.getCode());
        agent.setStatus("OFFLINE");
        agent.setType(request.getType());
        agent.setConfig(request.getConfig());
        agent.setSyncEnabled(false);
        agent.setApiKey(apiKeyUtil.generateApiKey());
        Agent createdAgent = agentService.create(agent);

        AgentCreateResponse response = new AgentCreateResponse();
        response.setId(createdAgent.getId());
        response.setUserId(createdAgent.getUserId());
        response.setName(createdAgent.getName());
        response.setCode(createdAgent.getCode());
        response.setStatus(createdAgent.getStatus());
        response.setType(createdAgent.getType());
        response.setConfig(createdAgent.getConfig());
        response.setApiKey(createdAgent.getApiKey());
        response.setApiKeyHash(createdAgent.getApiKeyHash());
        response.setChallengeVerified(createdAgent.getChallengeVerified());
        return Result.ok(response);
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Update an agent", description = "Update an existing agent's information")
    public ResponseEntity<Result<Agent>> updateAgent(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody Agent agent,
            HttpServletRequest httpRequest) {
        Long currentAgentId = (Long) httpRequest.getAttribute("agentId");
        if (currentAgentId == null || !currentAgentId.equals(id)) {
            throw new BusinessException(403, "Only the owning agent can update this agent");
        }
        agent.setId(id);
        Agent updatedAgent = agentService.update(agent);
        return Result.ok(updatedAgent);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    @Operation(summary = "Delete an agent", description = "Delete an agent by its ID")
    public ResponseEntity<Result<Void>> deleteAgent(@Parameter(description = "Agent ID") @PathVariable @Min(1) Long id) {
        agentService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    @RequireAuth
    @Operation(summary = "Get agent by ID", description = "Retrieve a specific agent by its ID")
    public ResponseEntity<Result<Agent>> getAgentById(@PathVariable @Min(1) Long id) {
        Agent agent = agentService.findById(id);
        return Result.ok(agent);
    }

    @GetMapping("/uid/{uid}")
    @ApiKeyAuth
    @Operation(summary = "Get agent by UID")
    public ResponseEntity<Result<Agent>> getAgentByUid(
            @Parameter(description = "Agent UID") @PathVariable String uid) {
        Agent agent = agentService.findByUid(uid);
        if (agent == null) {
            throw new BusinessException("Agent not found");
        }
        return Result.ok(agent);
    }

    @GetMapping("/code/{code}")
    @RequireAuth
    @Operation(summary = "Get agent by code", description = "Retrieve a specific agent by its code")
    public ResponseEntity<Result<Agent>> getAgentByCode(@PathVariable String code) {
        Agent agent = agentService.findByCode(code);
        return Result.ok(agent);
    }

    @GetMapping("/user/{userId}")
    @RequireAuth
    @Operation(summary = "Get agents by user", description = "Retrieve all agents owned by a specific user")
    public ResponseEntity<Result<List<Agent>>> getAgentsByUserId(@PathVariable Long userId) {
        List<Agent> agents = agentService.findByUserId(userId);
        return Result.ok(agents);
    }

    @GetMapping("/{id}/stats")
    @RequireAuth
    @Operation(summary = "Get agent statistics", description = "Retrieve statistics for a specific agent")
    public ResponseEntity<Result<AgentStatsResponse>> getAgentStats(@PathVariable @Min(1) Long id) {
        AgentStatsResponse stats = agentService.getStats(id);
        return Result.ok(stats);
    }

    @PostMapping("/{id}/heartbeat")
    @ApiKeyAuth
    @Operation(summary = "Send agent heartbeat", description = "Update agent heartbeat status")
    public ResponseEntity<Result<Void>> heartbeat(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody HeartbeatRequest request,
            HttpServletRequest httpRequest) {
        Long currentAgentId = (Long) httpRequest.getAttribute("agentId");
        if (currentAgentId == null || !currentAgentId.equals(id)) {
            throw new BusinessException(403, "Only the owning agent can send heartbeat");
        }
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        agentService.updateHeartbeat(id, request.getStatus(), now, request.getTimezone());
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @ApiKeyAuth
    @Operation(summary = "Update agent status", description = "Update the status of an agent")
    public ResponseEntity<Result<Void>> updateStatus(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long currentAgentId = (Long) httpRequest.getAttribute("agentId");
        if (currentAgentId == null || !currentAgentId.equals(id)) {
            throw new BusinessException(403, "Only the owning agent can update status");
        }
        agentService.updateStatusOnly(id, request.getStatus());
        return Result.ok();
    }

    @PutMapping("/{id}/config")
    @ApiKeyAuth
    @Operation(summary = "Update agent config", description = "Update the configuration of an agent")
    public ResponseEntity<Result<Void>> updateConfig(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody ConfigUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long currentAgentId = (Long) httpRequest.getAttribute("agentId");
        if (currentAgentId == null || !currentAgentId.equals(id)) {
            throw new BusinessException(403, "Only the owning agent can update config");
        }
        agentService.updateConfigOnly(id, request.getConfig());
        return Result.ok();
    }

    @GetMapping("/counts")
    @RequireAuth
    @Operation(summary = "Get resource counts for agents", description = "Get skill and memory counts for multiple agents by their IDs")
    public ResponseEntity<Result<Map<Long, AgentResourceCounts>>> getAgentCounts(
            @Parameter(description = "Comma-separated list of agent IDs") @RequestParam List<Long> agentIds) {
        Map<Long, AgentResourceCounts> counts = agentService.getResourceCounts(agentIds);
        return Result.ok(counts);
    }

    @PostMapping("/{id}/avatar")
    @ApiKeyAuth
    @Operation(summary = "Upload agent avatar", description = "Upload an avatar image for an agent")
    public ResponseEntity<Result<Map<String, String>>> uploadAvatar(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "Avatar image file") @RequestParam("avatar") MultipartFile file,
            HttpServletRequest request) {

        Long currentAgentId = (Long) request.getAttribute("agentId");
        if (currentAgentId == null || !currentAgentId.equals(id)) {
            return Result.fail(403, "Access denied");
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
            return Result.fail(400, "Only image files (jpg, png, gif, webp, svg, bmp) are allowed");
        }

        try {
            boolean preserveAlpha = "png".equals(extension) || "gif".equals(extension) || "webp".equals(extension);
            String outputFormat = preserveAlpha ? "png" : "jpg";
            if ("svg".equals(extension) || "bmp".equals(extension)) {
                outputFormat = "png";
            }

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                return Result.fail(400, "Unable to read image file");
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
            String fileName = id + "_" + System.currentTimeMillis() + "." + extForFile;
            String avatarDir = basePath + "/agents/" + id;
            Files.createDirectories(Paths.get(avatarDir));
            Files.write(Paths.get(avatarDir, fileName), baos.toByteArray());

            String avatarUrl = "/avatars/agents/" + id + "/" + fileName;
            agentService.updateAvatar(id, avatarUrl);

            Map<String, String> result = new HashMap<>();
            result.put("avatar", avatarUrl);
            return Result.ok(result);
        } catch (IOException e) {
            throw new BusinessException("Failed to upload avatar: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/avatar/{fileName}")
    @Operation(summary = "Get agent avatar image", description = "Retrieve an agent's avatar image by file name")
    public ResponseEntity<org.springframework.core.io.Resource> getAvatar(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "File name") @PathVariable String fileName) {
        try {
            Path filePath = Paths.get(basePath, "agents", String.valueOf(id), fileName).normalize();
            org.springframework.core.io.Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = getContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/sync")
    @ApiKeyAuth
    @Operation(summary = "Sync agent data", description = "Synchronize agent data since a specific timestamp")
    public ResponseEntity<Result<AgentSyncResponse>> syncData(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "Since timestamp (ISO format)") @RequestParam(required = false) String since,
            HttpServletRequest httpRequest) {
        Long currentAgentId = (Long) httpRequest.getAttribute("agentId");
        if (currentAgentId == null || !currentAgentId.equals(id)) {
            throw new BusinessException(403, "Only the owning agent can sync data");
        }
        AgentSyncResponse response = agentService.syncData(id, since);
        return Result.ok(response);
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
