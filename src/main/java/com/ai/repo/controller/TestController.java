package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.common.Result;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.User;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.MemoryService;
import com.ai.repo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/test")
@Tag(name = "Test API", description = "Testing辅助接口 - 仅用于开发和测试")
public class TestController {

    @Resource
    private UserService userService;

    @Resource
    private AgentService agentService;

    @Resource
    private MemoryService memoryService;

    @DeleteMapping("/agents")
    @Operation(summary = "删除所有Agent", description = "测试用：删除当前用户的所有Agent")
    public ResponseEntity<Result<Map<String, Object>>> deleteAllAgents() {
        int deletedCount = 0;
        try {
            var agents = agentService.findAll();
            for (Agent agent : agents) {
                if (agent.getUserId() != null) {
                    agentService.delete(agent.getId());
                    deletedCount++;
                }
            }
        } catch (Exception e) {
            return Result.fail("删除Agent失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deletedCount);
        return Result.ok(result);
    }

    @DeleteMapping("/agents/by-code/{code}")
    @Operation(summary = "按code删除Agent", description = "按Agent code删除指定的Agent")
    public ResponseEntity<Result<Map<String, Object>>> deleteAgentByCode(@PathVariable String code) {
        try {
            var agents = agentService.findAll();
            for (Agent agent : agents) {
                if (code.equals(agent.getCode())) {
                    agentService.delete(agent.getId());
                    Map<String, Object> result = new HashMap<>();
                    result.put("deleted", true);
                    result.put("agentId", agent.getId());
                    result.put("agentCode", code);
                    return Result.ok(result);
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("deleted", false);
            result.put("message", "Agent not found: " + code);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("删除Agent失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/users/{username}")
    @Operation(summary = "删除用户", description = "按用户名删除用户")
    public ResponseEntity<Result<Map<String, Object>>> deleteUser(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("deleted", false);
                result.put("message", "User not found: " + username);
                return Result.ok(result);
            }

            boolean deleted = userService.delete(user.getId());
            Map<String, Object> result = new HashMap<>();
            result.put("deleted", deleted);
            result.put("userId", user.getId());
            result.put("username", username);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("删除用户失败: " + e.getMessage());
        }
    }

    @PostMapping("/reset")
    @Operation(summary = "重置测试数据", description = "清理测试用户及其所有关联数据")
    public ResponseEntity<Result<Map<String, Object>>> resetTestData(@RequestParam(defaultValue = "testuser") String prefix) {
        Map<String, Object> result = new HashMap<>();
        int usersDeleted = 0;
        int agentsDeleted = 0;

        try {
            var allUsers = userService.findAll();
            for (User user : allUsers) {
                if (user.getUsername().startsWith(prefix) || user.getUsername().contains("test")) {
                    var userAgents = agentService.findAll().stream()
                            .filter(a -> a.getUserId() != null && a.getUserId().equals(user.getId()))
                            .toList();
                    for (Agent agent : userAgents) {
                        agentService.delete(agent.getId());
                        agentsDeleted++;
                    }
                    userService.delete(user.getId());
                    usersDeleted++;
                }
            }

            result.put("reset", true);
            result.put("usersDeleted", usersDeleted);
            result.put("agentsDeleted", agentsDeleted);
            return Result.ok(result);
        } catch (Exception e) {
            result.put("reset", false);
            result.put("error", e.getMessage());
            return Result.fail("重置失败: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "测试连接状态", description = "检查测试接口是否可用")
    public ResponseEntity<Result<Map<String, Object>>> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("service", "Test API");
        result.put("timestamp", System.currentTimeMillis());
        return Result.ok(result);
    }
}