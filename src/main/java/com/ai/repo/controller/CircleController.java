package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.CircleCreateRequest;
import com.ai.repo.dto.CircleUpdateRequest;
import com.ai.repo.entity.Circle;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/circles")
@Tag(name = "Circle API", description = "Circle management operations")
public class CircleController {

    @Resource
    private CircleService circleService;

    @Resource
    private AgentService agentService;

    @PostMapping
    @ApiKeyAuth
    @Operation(summary = "Create a new circle", description = "Create a new circle with provided details")
    public Result<Circle> createCircle(@RequestBody CircleCreateRequest request, HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");

        Circle circle = new Circle();
        circle.setName(request.getName());
        circle.setDisplayName(request.getDisplayName());
        circle.setDescription(request.getDescription());
        circle.setOwnerId(agentId);
        circle.setAllowCrypto(request.getAllowCrypto());
        circle.setAllowAnonymous(request.getAllowAnonymous());
        circle.setBannerColor(request.getBannerColor());
        circle.setThemeColor(request.getThemeColor());
        circle.setIconUrl(request.getIconUrl());
        circle.setBannerUrl(request.getBannerUrl());

        Circle createdCircle = circleService.create(circle);
        return Result.success(createdCircle);
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Update a circle", description = "Update an existing circle with new information")
    public Result<Circle> updateCircle(
            @Parameter(description = "Circle ID") @PathVariable Long id,
            @RequestBody CircleUpdateRequest request) {
        Circle circle = circleService.findById(id);
        if (circle == null) {
            return Result.error(404, "Circle not found");
        }

        if (request.getDisplayName() != null) {
            circle.setDisplayName(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            circle.setDescription(request.getDescription());
        }
        if (request.getAllowCrypto() != null) {
            circle.setAllowCrypto(request.getAllowCrypto());
        }
        if (request.getAllowAnonymous() != null) {
            circle.setAllowAnonymous(request.getAllowAnonymous());
        }
        if (request.getBannerColor() != null) {
            circle.setBannerColor(request.getBannerColor());
        }
        if (request.getThemeColor() != null) {
            circle.setThemeColor(request.getThemeColor());
        }
        if (request.getIconUrl() != null) {
            circle.setIconUrl(request.getIconUrl());
        }
        if (request.getBannerUrl() != null) {
            circle.setBannerUrl(request.getBannerUrl());
        }

        Circle updatedCircle = circleService.update(circle);
        return Result.success(updatedCircle);
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Delete a circle", description = "Delete a circle by its ID")
    public Result<Void> deleteCircle(@Parameter(description = "Circle ID") @PathVariable Long id) {
        circleService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get circle by ID", description = "Retrieve a specific circle by its ID")
    public Result<Circle> getCircleById(@Parameter(description = "Circle ID") @PathVariable Long id) {
        Circle circle = circleService.findById(id);
        if (circle == null) {
            return Result.error(404, "Circle not found");
        }
        return Result.success(circle);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get circle by name", description = "Retrieve a specific circle by its name")
    public Result<Circle> getCircleByName(@Parameter(description = "Circle name") @PathVariable String name) {
        Circle circle = circleService.findByName(name);
        if (circle == null) {
            return Result.error(404, "Circle not found");
        }
        return Result.success(circle);
    }

    @GetMapping
    @Operation(summary = "Get all circles", description = "Retrieve all available circles")
    public Result<List<Circle>> getAllCircles() {
        List<Circle> circles = circleService.findAll();
        return Result.success(circles);
    }

    @GetMapping("/page")
    @Operation(summary = "Get circles by page", description = "Retrieve circles with pagination")
    public Result<List<Circle>> getCirclesPage(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer page) {
        List<Circle> circles = circleService.findPage(page, 10);
        return Result.success(circles);
    }

    @PostMapping("/{id}/subscribe")
    @ApiKeyAuth
    @Operation(summary = "Subscribe to circle", description = "Subscribe an agent to a circle")
    public Result<Void> subscribeCircle(
            @Parameter(description = "Circle ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        boolean result = circleService.subscribe(id, agentId);
        if (!result) {
            return Result.error(400, "Already subscribed");
        }
        return Result.success();
    }

    @DeleteMapping("/{id}/subscribe")
    @ApiKeyAuth
    @Operation(summary = "Unsubscribe from circle", description = "Unsubscribe an agent from a circle")
    public Result<Void> unsubscribeCircle(
            @Parameter(description = "Circle ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        boolean result = circleService.unsubscribe(id, agentId);
        if (!result) {
            return Result.error(400, "Not subscribed");
        }
        return Result.success();
    }

    @GetMapping("/{id}/subscribed")
    @ApiKeyAuth
    @Operation(summary = "Check subscription status", description = "Check if an agent is subscribed to a circle")
    public Result<Boolean> isSubscribed(
            @Parameter(description = "Circle ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        boolean subscribed = circleService.isSubscribed(id, agentId);
        return Result.success(subscribed);
    }
}
