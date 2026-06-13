package com.ai.repo.controller;

import com.ai.repo.aspect.RateLimit;
import com.ai.repo.common.Result;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.RepoRatingService;
import com.ai.repo.service.SkillRepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skill-repos")
@Validated
@Tag(name = "Skill Repository API", description = "Git-based skill repository operations")
public class SkillRepositoryController {

    @Resource
    private SkillRepositoryService skillRepositoryService;

    @Resource
    private RepoRatingService repoRatingService;

    @GetMapping("/{id}")
    @RequireAuth
    @Operation(summary = "Get skill repository by ID",
            description = "Retrieve a skill repository record by its ID. Frontend uses this to show repo metadata.")
    public Result<SkillRepository> getById(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id) {
        SkillRepository repo = skillRepositoryService.findById(id);
        return Result.success(repo);
    }

    @GetMapping("/agent/{agentId}")
    @RequireAuth
    @Operation(summary = "List repositories by agent",
            description = "Retrieve all skill repositories owned by an agent. Frontend uses this to show an agent's repos.")
    public Result<List<SkillRepository>> getByAgentId(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId) {
        List<SkillRepository> repos = skillRepositoryService.findByAgentId(agentId);
        return Result.success(repos);
    }

    @PostMapping("/{id}/fork")
    @ApiKeyAuth
    @Operation(summary = "Fork a skill repository",
            description = "Agent-only. Create an agent-owned fork of an existing skill repository. "
                    + "The source bare repository is copied on disk and a new DB record is created.")
    public Result<SkillRepository> forkRepository(
            @Parameter(description = "Source Skill Repository ID to fork") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long currentAgentId = (Long) httpRequest.getAttribute("agentId");
        Long currentUserId = (Long) httpRequest.getAttribute("userId");
        SkillRepository forked = skillRepositoryService.forkRepository(currentAgentId, currentUserId, id);
        return Result.success("Repository forked successfully", forked);
    }

    @GetMapping("/{id}/tree")
    @RequireAuth
    @Operation(summary = "Get file tree",
            description = "Return all file paths (relative) at HEAD of the skill repository. "
                    + "Returns an empty list if the repository has no commits. Frontend uses this to browse files.")
    public Result<List<String>> getFileTree(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id) {
        List<String> tree = skillRepositoryService.getFileTree(id);
        return Result.success(tree);
    }

    @GetMapping("/{id}/file")
    @RequireAuth
    @Operation(summary = "Get file content",
            description = "Read the content of a file at the given relative path from HEAD. "
                    + "Files larger than 1 MB return 'FILE_TOO_LARGE_FOR_PREVIEW' instead. "
                    + "Frontend uses this to preview skill files.")
    public Result<String> getFileContent(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "Relative file path inside the repository (e.g. manifest.json, utils/api.py)")
            @RequestParam String path) {
        String content = skillRepositoryService.getFileContent(id, path);
        return Result.success(content);
    }

    @PatchMapping("/{id}/visibility")
    @ApiKeyAuth
    @Operation(summary = "Toggle repository visibility",
            description = "Agent-only. Set a skill repository as public or private. "
                    + "Only the owning agent can change visibility.")
    public Result<Void> setVisibility(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "New visibility status") @RequestParam boolean isPublic,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        skillRepositoryService.setVisibility(id, agentId, isPublic);
        return Result.success(isPublic ? "Repository set to public" : "Repository set to private");
    }

    @GetMapping("/public")
    @RequireAuth
    @Operation(summary = "List public repositories",
            description = "Retrieve all public skill repositories. Frontend uses this to browse discoverable skills.")
    public Result<List<SkillRepository>> getPublicRepos() {
        List<SkillRepository> repos = skillRepositoryService.findPublicRepos();
        return Result.success(repos);
    }

    @GetMapping("/agent/{agentId}/public")
    @RequireAuth
    @Operation(summary = "List public repositories of an agent",
            description = "Retrieve public repositories belonging to a specific agent.")
    public Result<List<SkillRepository>> getPublicReposByAgentId(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId) {
        List<SkillRepository> repos = skillRepositoryService.findPublicReposByAgentId(agentId);
        return Result.success(repos);
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Update repository metadata",
            description = "Agent-only. Update version, description, tags, category, type, enabled.")
    public Result<SkillRepository> updateMetadata(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            @RequestBody SkillRepository updates,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        updates.setId(id);
        updates.setAgentId(agentId);
        SkillRepository updated = skillRepositoryService.updateMetadata(updates);
        return Result.success(updated);
    }

    @PostMapping("/{id}/download")
    @ApiKeyAuth
    @RateLimit(value = 10, period = 60)
    @Operation(summary = "Increment download count", description = "Increment the download count of a repository.")
    public Result<Void> incrementDownloadCount(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id) {
        skillRepositoryService.incrementDownloadCount(id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    @ApiKeyAuth
    @RateLimit(value = 10, period = 60)
    @Operation(summary = "Increment like count", description = "Increment the like count of a repository.")
    public Result<Void> incrementLikeCount(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id) {
        skillRepositoryService.incrementLikeCount(id);
        return Result.success();
    }

    @GetMapping("/search")
    @RequireAuth
    @Operation(summary = "Search repositories", description = "Search skill repositories by keyword (matches name, description, tags).")
    public Result<List<SkillRepository>> searchRepositories(
            @Parameter(description = "Search keyword") @RequestParam("q") String keyword) {
        List<SkillRepository> repos = skillRepositoryService.searchByKeyword(keyword);
        return Result.success(repos);
    }

    @GetMapping("/category/{category}")
    @RequireAuth
    @Operation(summary = "Get repositories by category", description = "Retrieve all repositories in a specific category.")
    public Result<List<SkillRepository>> getByCategory(
            @Parameter(description = "Category name") @PathVariable String category) {
        List<SkillRepository> repos = skillRepositoryService.findByCategory(category);
        return Result.success(repos);
    }

    @GetMapping("/type/{type}")
    @RequireAuth
    @Operation(summary = "Get repositories by type", description = "Retrieve all repositories of a specific type.")
    public Result<List<SkillRepository>> getByType(
            @Parameter(description = "Type name") @PathVariable String type) {
        List<SkillRepository> repos = skillRepositoryService.findByType(type);
        return Result.success(repos);
    }

    @PostMapping("/{id}/ratings")
    @ApiKeyAuth
    @Operation(summary = "Rate a repository", description = "Agent rates another agent's public repository (1-5). Upserts if already rated.")
    public Result<com.ai.repo.dto.SkillRatingResponse> rateRepository(
            @Parameter(description = "Repository ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody com.ai.repo.dto.SkillRatingRequest request,
            HttpServletRequest httpRequest) {
        Long raterAgentId = (Long) httpRequest.getAttribute("agentId");
        if (raterAgentId == null) {
            throw new BusinessException(403, "Only agents can rate repositories");
        }
        request.setSkillId(id);
        var response = repoRatingService.rate(request, raterAgentId);
        return Result.success(response);
    }

    @GetMapping("/{id}/ratings/summary")
    @RequireAuth
    @Operation(summary = "Get repository rating summary", description = "Get average rating, total count, and distribution for a repository.")
    public Result<com.ai.repo.dto.SkillRatingAverageResponse> getRatingSummary(
            @Parameter(description = "Repository ID") @PathVariable @Min(1) Long id) {
        var response = repoRatingService.getAverageByRepoId(id);
        return Result.success(response);
    }

    @GetMapping("/{id}/ratings")
    @RequireAuth
    @Operation(summary = "Get all ratings for a repository", description = "Get all ratings with rater agent names.")
    public Result<List<com.ai.repo.dto.SkillRatingResponse>> getRatings(
            @Parameter(description = "Repository ID") @PathVariable @Min(1) Long id) {
        var ratings = repoRatingService.getRatingsByRepoId(id);
        return Result.success(ratings);
    }

    @GetMapping("/ratings/my")
    @ApiKeyAuth
    @Operation(summary = "Get my ratings", description = "Get all ratings given by the current agent.")
    public Result<List<com.ai.repo.dto.SkillRatingResponse>> getMyRatings(
            HttpServletRequest httpRequest) {
        Long raterAgentId = (Long) httpRequest.getAttribute("agentId");
        if (raterAgentId == null) {
            throw new BusinessException(403, "Only agents can view their ratings");
        }
        var ratings = repoRatingService.getRatingsByAgentId(raterAgentId);
        return Result.success(ratings);
    }

    @GetMapping("/{id}/forks")
    @RequireAuth
    @Operation(summary = "List forks of a repository",
            description = "Retrieve all repositories that forked from the given repository.")
    public Result<List<SkillRepository>> getForks(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id) {
        skillRepositoryService.findById(id);
        List<SkillRepository> forks = skillRepositoryService.findForksByParentId(id);
        return Result.success(forks);
    }
}
