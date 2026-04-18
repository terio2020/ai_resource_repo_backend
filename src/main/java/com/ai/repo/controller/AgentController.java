package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.common.Result;
import com.ai.repo.dto.*;
import com.ai.repo.entity.Agent;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.AgentService;
import com.ai.repo.util.ApiKeyUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/agents")
@Tag(name = "Agent API", description = "Agent management operations")
public class AgentController {

    @Resource
    private AgentService agentService;

    @Resource
    private ApiKeyUtil apiKeyUtil;

    @PostMapping
    @RequireAuth
    @Operation(summary = "Create a new agent", description = "Create a new agent for the authenticated user")
    public Result<Agent> createAgent(@RequestBody AgentCreateRequest request, HttpServletRequest httpRequest) {
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
        return Result.success(createdAgent);
    }

    @PutMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    @Operation(summary = "Update an agent", description = "Update an existing agent's information")
    public Result<Agent> updateAgent(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @RequestBody Agent agent) {
        agent.setId(id);
        Agent updatedAgent = agentService.update(agent);
        return Result.success(updatedAgent);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    @Operation(summary = "Delete an agent", description = "Delete an agent by its ID")
    public Result<Void> deleteAgent(@Parameter(description = "Agent ID") @PathVariable Long id) {
        agentService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agent by ID", description = "Retrieve a specific agent by its ID")
    public Result<Agent> getAgentById(@Parameter(description = "Agent ID") @PathVariable Long id) {
        Agent agent = agentService.findById(id);
        return Result.success(agent);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get agent by code", description = "Retrieve a specific agent by its code")
    public Result<Agent> getAgentByCode(@Parameter(description = "Agent code") @PathVariable String code) {
        Agent agent = agentService.findByCode(code);
        return Result.success(agent);
    }

    @GetMapping
    @Operation(summary = "Get all agents", description = "Retrieve all available agents")
    public Result<List<Agent>> getAllAgents() {
        List<Agent> agents = agentService.findAll();
        return Result.success(agents);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get agents by user", description = "Retrieve all agents owned by a specific user")
    public Result<List<Agent>> getAgentsByUserId(@Parameter(description = "User ID") @PathVariable Long userId) {
        List<Agent> agents = agentService.findByUserId(userId);
        return Result.success(agents);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get agents by status", description = "Retrieve all agents with a specific status")
    public Result<List<Agent>> getAgentsByStatus(@Parameter(description = "Agent status") @PathVariable String status) {
        List<Agent> agents = agentService.findByStatus(status);
        return Result.success(agents);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get agents by type", description = "Retrieve all agents of a specific type")
    public Result<List<Agent>> getAgentsByType(@Parameter(description = "Agent type") @PathVariable String type) {
        List<Agent> agents = agentService.findByType(type);
        return Result.success(agents);
    }

    @GetMapping("/page")
    @RequireAuth
    @Operation(summary = "Get agents by page", description = "Retrieve authenticated user's agents with pagination")
    public Result<PageResult<Agent>> getAgentsByPage(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PageResult<Agent> pageResult = agentService.findPageByUserId(userId, page, size);
        return Result.success(pageResult);
    }

    @PostMapping("/search")
    @Operation(summary = "Search agents", description = "Search agents by various criteria")
    public Result<List<Agent>> searchAgents(@RequestBody AgentSearchRequest request) {
        List<Agent> agents = agentService.findBySearch(request);
        return Result.success(agents);
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get agent statistics", description = "Retrieve statistics for a specific agent")
    public Result<AgentStatsResponse> getAgentStats(@Parameter(description = "Agent ID") @PathVariable Long id) {
        AgentStatsResponse stats = agentService.getStats(id);
        return Result.success(stats);
    }

    @PostMapping("/{id}/heartbeat")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    @Operation(summary = "Send agent heartbeat", description = "Update agent heartbeat status")
    public Result<Void> heartbeat(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @RequestBody HeartbeatRequest request) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        agentService.updateHeartbeat(id, request.getStatus(), now);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    @Operation(summary = "Update agent status", description = "Update the status of an agent")
    public Result<Void> updateStatus(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        agentService.updateStatusOnly(id, request.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/config")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    @Operation(summary = "Update agent config", description = "Update the configuration of an agent")
    public Result<Void> updateConfig(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @RequestBody ConfigUpdateRequest request) {
        agentService.updateConfigOnly(id, request.getConfig());
        return Result.success();
    }

    @GetMapping("/{id}/sync")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    @Operation(summary = "Sync agent data", description = "Synchronize agent data since a specific timestamp")
    public Result<AgentSyncResponse> syncData(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @Parameter(description = "Since timestamp (ISO format)") @RequestParam(required = false) String since) {
        AgentSyncResponse response = agentService.syncData(id, since);
        return Result.success(response);
    }
}
