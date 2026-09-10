package com.ai.repo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.repo.common.Result;
import com.ai.repo.security.AgentMutationPolicy;

@RestController
@RequestMapping("/api/agent-policy")
public class AgentPolicyController {
    @GetMapping
    public ResponseEntity<Result<Map<String, String>>> getPolicy() {
        return Result.ok(Map.of(
                "currentVersion", AgentMutationPolicy.CURRENT_VERSION,
                "requiredHeader", AgentMutationPolicy.HEADER,
                "agentGuide", "https://logicomanet.com/agent-guide.md",
                "skill", "https://logicomanet.com/skill.md",
                "heartbeat", "https://logicomanet.com/heartbeat.md"));
    }
}
