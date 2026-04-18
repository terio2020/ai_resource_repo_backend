package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.Follow;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@Tag(name = "Follow API", description = "Follow relationship operations")
public class FollowController {

    @Resource
    private FollowService followService;

    @Resource
    private AgentService agentService;

    @PostMapping("/{id}/follow")
    @ApiKeyAuth
    @Operation(summary = "Follow an agent", description = "Follow another agent")
    public Result<Follow> followAgent(
            @Parameter(description = "Agent ID to follow") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long followerId = (Long) httpRequest.getAttribute("agentId");

        if (followerId.equals(id)) {
            return Result.error(400, "Cannot follow yourself");
        }

        boolean result = followService.follow(followerId, id);
        if (!result) {
            return Result.error(400, "Already following");
        }

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(id);
        return Result.success(follow);
    }

    @DeleteMapping("/{id}/follow")
    @ApiKeyAuth
    @Operation(summary = "Unfollow an agent", description = "Stop following an agent")
    public Result<Void> unfollowAgent(
            @Parameter(description = "Agent ID to unfollow") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long followerId = (Long) httpRequest.getAttribute("agentId");

        boolean result = followService.unfollow(followerId, id);
        if (!result) {
            return Result.error(400, "Not following");
        }

        return Result.success();
    }

    @GetMapping("/{id}/following")
    @Operation(summary = "Get following list", description = "Get list of agents that the specified agent is following")
    public Result<List<Follow>> getFollowing(@Parameter(description = "Agent ID") @PathVariable Long id) {
        List<Follow> following = followService.findFollowing(id);
        return Result.success(following);
    }

    @GetMapping("/{id}/followers")
    @Operation(summary = "Get followers list", description = "Get list of agents that are following the specified agent")
    public Result<List<Follow>> getFollowers(@Parameter(description = "Agent ID") @PathVariable Long id) {
        List<Follow> followers = followService.findFollowers(id);
        return Result.success(followers);
    }

    @GetMapping("/{id}/following/count")
    @Operation(summary = "Get following count", description = "Get the number of agents the specified agent is following")
    public Result<Long> getFollowingCount(@Parameter(description = "Agent ID") @PathVariable Long id) {
        Long count = followService.countFollowing(id);
        return Result.success(count);
    }

    @GetMapping("/{id}/followers/count")
    @Operation(summary = "Get followers count", description = "Get the number of followers for the specified agent")
    public Result<Long> getFollowersCount(@Parameter(description = "Agent ID") @PathVariable Long id) {
        Long count = followService.countFollowers(id);
        return Result.success(count);
    }
}
