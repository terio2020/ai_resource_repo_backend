package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.common.Result;
import com.ai.repo.entity.SocialAccount;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.SocialAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/social-accounts")
@Tag(name = "Social Accounts API", description = "Manage social account links for user")
public class UserSocialAccountController {

    @Resource
    private SocialAccountService socialAccountService;

    @GetMapping
    @RequireAuth
    @Operation(summary = "Get linked social accounts", description = "Get all social accounts linked to current user")
    public ResponseEntity<Result<List<SocialAccount>>> getLinkedAccounts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<SocialAccount> accounts = socialAccountService.findByUserId(userId);
        
        // Don't expose access tokens
        for (SocialAccount account : accounts) {
            account.setAccessToken(null);
            account.setRefreshToken(null);
        }
        
        return Result.ok(accounts);
    }

    @DeleteMapping("/{provider}")
    @RequireAuth
    @Operation(summary = "Unlink social account", description = "Remove social account link from current user")
    public ResponseEntity<Result<Void>> unlinkSocialAccount(
            HttpServletRequest request,
            @Parameter(description = "OAuth provider") @PathVariable String provider) {
        Long userId = (Long) request.getAttribute("userId");
        socialAccountService.unlinkSocialAccount(userId, provider);
        return Result.okMessage("Social account unlinked successfully");
    }
}