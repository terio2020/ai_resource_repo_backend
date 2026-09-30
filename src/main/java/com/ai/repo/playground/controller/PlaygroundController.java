package com.ai.repo.playground.controller;

import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ai.repo.common.Result;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.playground.dto.PlaygroundRequests.*;
import com.ai.repo.playground.entity.PlaygroundRows.Participation;
import com.ai.repo.playground.service.PlaygroundService;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/playground")
@Validated
@ConditionalOnProperty(name="playground.enabled",havingValue="true")
public class PlaygroundController {
    private final PlaygroundService service;
    public PlaygroundController(PlaygroundService service) { this.service=service; }
    @GetMapping("/agents/{agentId}/participation") @RequireAuth
    public ResponseEntity<Result<Participation>> participation(@PathVariable @Min(1) long agentId,HttpServletRequest request) {
        return Result.ok(service.participation(human(request),agentId));
    }
    @PutMapping("/agents/{agentId}/participation") @RequireAuth
    public ResponseEntity<Result<Participation>> participation(@PathVariable @Min(1) long agentId,
            @Valid @RequestBody ParticipationUpdate body,HttpServletRequest request) {
        return Result.ok(service.updateParticipation(human(request),agentId,body));
    }
    @GetMapping("/mine") @RequireAuth
    public ResponseEntity<Result<List<Map<String,Object>>>> mine(
            @RequestParam(defaultValue="9223372036854775807") @Min(1) long beforeId,HttpServletRequest request) {
        return Result.ok(service.mine(human(request),beforeId));
    }
    @GetMapping("/agent/opportunities") @ApiKeyAuth
    public ResponseEntity<Result<List<Map<String,Object>>>> opportunities(HttpServletRequest request) {
        return Result.ok(service.opportunities(agent(request)));
    }
    @PostMapping("/intentions") @RequireAuth
    public ResponseEntity<Result<Map<String,Object>>> invite(@Valid @RequestBody Invitation body,HttpServletRequest request) {
        return Result.ok(service.invite(human(request),body));
    }
    @PostMapping("/activities/{activityId}/invitation/accept") @RequireAuth
    public ResponseEntity<Result<Map<String,Object>>> accept(@PathVariable @Min(1) long activityId,
            @Valid @RequestBody InvitationAccept body,HttpServletRequest request) {
        return Result.ok(service.acceptInvitation(human(request),activityId,body));
    }
    @GetMapping("/activities/{activityId}") @RequireAuth
    public ResponseEntity<Result<ObjectNode>> activity(@PathVariable @Min(1) long activityId,HttpServletRequest request) {
        return Result.ok(service.ownerActivity(human(request),activityId));
    }
    @GetMapping("/activities/{activityId}/events") @RequireAuth
    public ResponseEntity<Result<List<JsonNode>>> events(@PathVariable @Min(1) long activityId,
            @RequestParam(defaultValue="0") @Min(0) long afterSequence,HttpServletRequest request) {
        return Result.ok(service.ownerEvents(human(request),activityId,afterSequence));
    }
    @PostMapping("/activities/{activityId}/leave") @RequireAuth
    public ResponseEntity<Result<Void>> leave(@PathVariable @Min(1) long activityId,HttpServletRequest request) {
        service.leave(human(request),activityId); return Result.ok();
    }
    @PostMapping("/agent/activities/{activityId}/join") @ApiKeyAuth
    public ResponseEntity<Result<Map<String,Object>>> join(@PathVariable @Min(1) long activityId,HttpServletRequest request) {
        return Result.ok(service.join(agent(request),activityId));
    }
    @GetMapping("/agent/tasks") @ApiKeyAuth
    public ResponseEntity<Result<List<Map<String,Object>>>> tasks(HttpServletRequest request) { return Result.ok(service.tasks(agent(request))); }
    @PostMapping("/agent/tasks/{taskId}/claim") @ApiKeyAuth
    public ResponseEntity<Result<ObjectNode>> claim(@PathVariable @Min(1) long taskId,
            @Valid @RequestBody Claim body,HttpServletRequest request) { return Result.ok(service.claim(agent(request),taskId,body)); }
    @PostMapping("/agent/tasks/{taskId}/attempts") @ApiKeyAuth
    public ResponseEntity<Result<ObjectNode>> attempt(@PathVariable @Min(1) long taskId,
            @Valid @RequestBody AttemptStart body,HttpServletRequest request) { return Result.ok(service.startAttempt(agent(request),taskId,body)); }
    @PostMapping("/agent/tasks/{taskId}/failures") @ApiKeyAuth
    public ResponseEntity<Result<ObjectNode>> failure(@PathVariable @Min(1) long taskId,
            @Valid @RequestBody AttemptFailure body,HttpServletRequest request) { return Result.ok(service.reportAttemptFailure(agent(request),taskId,body)); }
    @PostMapping("/agent/tasks/{taskId}/actions") @ApiKeyAuth
    public ResponseEntity<Result<JsonNode>> action(@PathVariable @Min(1) long taskId,
            @Valid @RequestBody Submission body,HttpServletRequest request) { return Result.ok(service.submit(agent(request),taskId,body)); }
    private long human(HttpServletRequest request) {
        if (request.getAttribute("agentId")!=null) throw new BusinessException(403,"HUMAN_JWT_REQUIRED");
        Object user=request.getAttribute("userId"); if (!(user instanceof Long id) || id<=0) throw new BusinessException(401,"AUTHENTICATION_REQUIRED"); return id;
    }
    private long agent(HttpServletRequest request) {
        Object actor=request.getAttribute("agentId"); if (!(actor instanceof Long id) || id<=0) throw new BusinessException(403,"AGENT_API_KEY_REQUIRED"); return id;
    }
}
