package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.SkillRatingAverageResponse;
import com.ai.repo.dto.SkillRatingRequest;
import com.ai.repo.dto.SkillRatingResponse;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.SkillRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Skill Rating API", description = "Skill rating operations for agents and frontend")
public class SkillRatingController {

    @Resource
    private SkillRatingService skillRatingService;

    @PostMapping("/skill-ratings")
    @ApiKeyAuth
    @Operation(summary = "Rate a public skill", description = "Agent rates another agent's public skill. If already rated, updates the existing rating (upsert).")
    public Result<SkillRatingResponse> rateSkill(
            @Valid @RequestBody SkillRatingRequest request,
            HttpServletRequest httpRequest) {
        Long raterAgentId = (Long) httpRequest.getAttribute("agentId");
        if (raterAgentId == null) {
            return Result.error(403, "Only agents can rate skills");
        }
        SkillRatingResponse response = skillRatingService.rate(request, raterAgentId);
        return Result.success(response);
    }

    @GetMapping("/skills/{skillId}/rating")
    @RequireAuth
    @Operation(summary = "Get skill rating average", description = "Get the average rating, total count, and distribution for a skill. Accessible by agents and frontend users.")
    public Result<SkillRatingAverageResponse> getSkillAverageRating(
            @PathVariable Long skillId) {
        SkillRatingAverageResponse response = skillRatingService.getAverageBySkillId(skillId);
        return Result.success(response);
    }

    @GetMapping("/skills/{skillId}/ratings")
    @RequireAuth
    @Operation(summary = "Get all ratings for a skill", description = "Get the list of all ratings for a skill with rater agent names.")
    public Result<List<SkillRatingResponse>> getSkillRatings(
            @PathVariable Long skillId) {
        List<SkillRatingResponse> ratings = skillRatingService.getRatingsBySkillId(skillId);
        return Result.success(ratings);
    }

    @GetMapping("/skill-ratings/my")
    @ApiKeyAuth
    @Operation(summary = "Get my ratings", description = "Get all ratings given by the current agent.")
    public Result<List<SkillRatingResponse>> getMyRatings(
            HttpServletRequest httpRequest) {
        Long raterAgentId = (Long) httpRequest.getAttribute("agentId");
        if (raterAgentId == null) {
            return Result.error(403, "Only agents can view their ratings");
        }
        List<SkillRatingResponse> ratings = skillRatingService.getRatingsByAgentId(raterAgentId);
        return Result.success(ratings);
    }
}
