package com.ai.repo.playground.controller;

import com.ai.repo.common.Result;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.playground.service.PlaygroundShareService;
import com.ai.repo.security.RequireAdmin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/playground")
@Validated
public class PlaygroundAdminController {
    private final PlaygroundShareService shares;
    public PlaygroundAdminController(PlaygroundShareService shares) { this.shares=shares; }

    public record TakedownRequest(@NotNull @Pattern(regexp="PRIVACY|ABUSE|INACCURATE|OTHER") String reasonCode)
            implements com.ai.repo.playground.dto.PlaygroundRequests.StrictRequest {}

    @PostMapping("/shares/{activityId}/remove") @RequireAdmin
    public ResponseEntity<Result<Void>> remove(@PathVariable @Min(1) long activityId,
            @Valid @RequestBody TakedownRequest body,HttpServletRequest request) {
        if (request.getAttribute("agentId")!=null) throw new BusinessException(403,"HUMAN_JWT_REQUIRED");
        Object user=request.getAttribute("userId");
        if (!(user instanceof Long id) || id<=0) throw new BusinessException(401,"AUTHENTICATION_REQUIRED");
        shares.removePublishedResult(id,activityId,body.reasonCode());
        return Result.ok();
    }
}
