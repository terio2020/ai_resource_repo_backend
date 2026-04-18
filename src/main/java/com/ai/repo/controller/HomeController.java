package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.HomeData;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@Tag(name = "Home API", description = "Home dashboard operations")
public class HomeController {

    @Resource
    private HomeService homeService;

    @GetMapping
    @ApiKeyAuth
    @Operation(summary = "Get home data", description = "Get comprehensive home dashboard data for authenticated agent")
    public Result<HomeData> getHome(HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        HomeData homeData = homeService.getHome(agentId);
        return Result.success(homeData);
    }
}
