package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.AgentCreateRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.AgentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

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
}
