package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.common.Result;
import com.ai.repo.dto.*;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.PackageContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@Validated
@Tag(name = "Package Contribution API", description = "Community contribution and review for packages")
public class PackageContributionController {

    @Resource
    private PackageContributionService packageContributionService;

    @PostMapping("/{id}/contributions")
    @RequireAuth
    @Operation(summary = "Submit a contribution",
            description = "Submit modified files as a contribution PR. Only for public packages. "
                    + "You cannot contribute to your own package.")
    public ResponseEntity<Result<ContributionResponse>> submitContribution(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id,
            @Valid @RequestParam("sourceVersionId") Long sourceVersionId,
            @RequestParam("commitMessage") String commitMessage,
            @Parameter(description = "Modified files") @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        ContributionResponse response = packageContributionService.submit(
                id, userId, agentId, sourceVersionId, commitMessage, files);
        return Result.ok("Contribution submitted", response);
    }

    @GetMapping("/{id}/contributions")
    @RequireAuth
    @Operation(summary = "List contributions", description = "List all contribution PRs for a package.")
    public ResponseEntity<Result<List<ContributionResponse>>> listContributions(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id) {
        List<ContributionResponse> list = packageContributionService.listByPackage(id);
        return Result.ok(list);
    }

    @GetMapping("/{id}/contributions/{contributionId}")
    @RequireAuth
    @Operation(summary = "Get contribution detail", description = "Get a single contribution PR with file list.")
    public ResponseEntity<Result<ContributionResponse>> getContribution(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "Contribution ID") @PathVariable @Min(1) Long contributionId) {
        ContributionResponse response = packageContributionService.getById(contributionId);
        return Result.ok(response);
    }

    @PutMapping("/{id}/contributions/{contributionId}")
    @RequireAuth
    @Operation(summary = "Review a contribution",
            description = "Approve or reject a contribution PR. Only the package owner can review.")
    public ResponseEntity<Result<ContributionResponse>> reviewContribution(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "Contribution ID") @PathVariable @Min(1) Long contributionId,
            @Valid @RequestBody ContributionReviewRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        ContributionResponse response = packageContributionService.review(
                id, contributionId, userId, request);
        return Result.ok("Contribution " + request.getStatus(), response);
    }
}
