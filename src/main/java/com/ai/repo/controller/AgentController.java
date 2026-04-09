package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.common.Result;
import com.ai.repo.dto.AgentCreateRequest;
import com.ai.repo.dto.AgentSearchRequest;
import com.ai.repo.dto.AgentStatsResponse;
import com.ai.repo.dto.AgentSyncResponse;
import com.ai.repo.dto.BatchDeleteRequest;
import com.ai.repo.dto.ConfigUpdateRequest;
import com.ai.repo.dto.HeartbeatRequest;
import com.ai.repo.dto.StatusUpdateRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.AgentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    @Resource
    private AgentService agentService;

    @PostMapping
    @RequireAuth
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
        Agent createdAgent = agentService.create(agent);
        return Result.success(createdAgent);
    }

    @PutMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    public Result<Agent> updateAgent(@PathVariable Long id, @RequestBody Agent agent) {
        agent.setId(id);
        Agent updatedAgent = agentService.update(agent);
        return Result.success(updatedAgent);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    public Result<Void> deleteAgent(@PathVariable Long id) {
        agentService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Agent> getAgentById(@PathVariable Long id) {
        Agent agent = agentService.findById(id);
        return Result.success(agent);
    }

    @GetMapping("/code/{code}")
    public Result<Agent> getAgentByCode(@PathVariable String code) {
        Agent agent = agentService.findByCode(code);
        return Result.success(agent);
    }

    @GetMapping
    public Result<List<Agent>> getAllAgents() {
        List<Agent> agents = agentService.findAll();
        return Result.success(agents);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Agent>> getAgentsByUserId(@PathVariable Long userId) {
        List<Agent> agents = agentService.findByUserId(userId);
        return Result.success(agents);
    }

    @GetMapping("/status/{status}")
    public Result<List<Agent>> getAgentsByStatus(@PathVariable String status) {
        List<Agent> agents = agentService.findByStatus(status);
        return Result.success(agents);
    }

    @GetMapping("/type/{type}")
    public Result<List<Agent>> getAgentsByType(@PathVariable String type) {
        List<Agent> agents = agentService.findByType(type);
        return Result.success(agents);
    }

    @GetMapping("/page")
    public Result<PageResult<Agent>> getAgentsByPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<Agent> pageResult = agentService.findPage(page, size);
        return Result.success(pageResult);
    }

    @PostMapping("/search")
    public Result<List<Agent>> searchAgents(@RequestBody AgentSearchRequest request) {
        List<Agent> agents = agentService.findBySearch(request);
        return Result.success(agents);
    }

    @GetMapping("/{id}/stats")
    public Result<AgentStatsResponse> getAgentStats(@PathVariable Long id) {
        AgentStatsResponse stats = agentService.getStats(id);
        return Result.success(stats);
    }

    @PostMapping("/{id}/heartbeat")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    public Result<Void> heartbeat(@PathVariable Long id, @RequestBody HeartbeatRequest request) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        agentService.updateHeartbeat(id, request.getStatus(), now);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        agentService.updateStatusOnly(id, request.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/config")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    public Result<Void> updateConfig(@PathVariable Long id, @RequestBody ConfigUpdateRequest request) {
        agentService.updateConfigOnly(id, request.getConfig());
        return Result.success();
    }

    @GetMapping("/{id}/sync")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "id")
    public Result<AgentSyncResponse> syncData(
            @PathVariable Long id,
            @RequestParam(required = false) String since) {
        AgentSyncResponse response = agentService.syncData(id, since);
        return Result.success(response);
    }
}