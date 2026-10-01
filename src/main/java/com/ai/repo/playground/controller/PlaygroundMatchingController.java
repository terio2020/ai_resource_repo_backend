package com.ai.repo.playground.controller;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ai.repo.common.Result;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.playground.dto.PlaygroundRequests.MatchJoin;
import com.ai.repo.playground.service.PlaygroundMatchingService;
@RestController @Validated @RequestMapping("/api/playground/matching")
@ConditionalOnProperty(name="playground.enabled",havingValue="true")
public class PlaygroundMatchingController {
    private final PlaygroundMatchingService service;
    public PlaygroundMatchingController(PlaygroundMatchingService service){this.service=service;}
    @PostMapping @RequireAuth public ResponseEntity<Result<Map<String,Object>>> join(@Valid @RequestBody MatchJoin body,HttpServletRequest request){return Result.ok(service.enqueue(human(request),body));}
    @GetMapping("/agents/{id}") @RequireAuth public ResponseEntity<Result<Map<String,Object>>> status(@PathVariable @Min(1) long id,HttpServletRequest request){return Result.ok(service.status(human(request),id));}
    @DeleteMapping("/agents/{id}") @RequireAuth public ResponseEntity<Result<Void>> cancel(@PathVariable @Min(1) long id,HttpServletRequest request){service.cancel(human(request),id);return Result.ok();}
    private long human(HttpServletRequest r){if(r.getAttribute("agentId")!=null)throw new BusinessException(403,"HUMAN_JWT_REQUIRED");if(!(r.getAttribute("userId") instanceof Long id) || id<=0)throw new BusinessException(401,"AUTHENTICATION_REQUIRED");return id;}
}
