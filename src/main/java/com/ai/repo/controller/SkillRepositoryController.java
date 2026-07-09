package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.aspect.RateLimit;
import com.ai.repo.common.Result;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.RepoRatingService;
import com.ai.repo.dto.FileTreeEntry;
import com.ai.repo.service.SkillRepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Resource
    private AgentService agentService;

    @GetMapping("/{id}")
    @RequireAuth
    @Operation(summary = "Get skill repository by ID",
            description = "Retrieve a skill repository record by its ID. Only returns the repo if it is public or the caller owns it.")
    public ResponseEntity<Result<SkillRepository>> getById(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        return Result.ok(repo);
    }

    @GetMapping("/shared/{shareId}")
    @RequireAuth
    @Operation(summary = "Get a public skill repository by share ID",
            description = "Retrieve a public skill repository by its share ID (hash). "
                    + "Only returns public repositories. Used for share links.")
    public ResponseEntity<Result<SkillRepository>> getByShareId(
            @Parameter(description = "Share ID (hash)") @PathVariable String shareId) {
        SkillRepository repo = skillRepositoryService.findByShareId(shareId);
        if (!Boolean.TRUE.equals(repo.getIsPublic())) {
            throw new com.ai.repo.exception.RepositoryNotFoundException("Skill not found");
        }
        return Result.ok(repo);
    }

    private void requireViewAccess(SkillRepository repo, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        boolean isOwner = (userId != null && userId.equals(repo.getUserId()))
                || (agentId != null && agentId.equals(repo.getAgentId()));
        if (!Boolean.TRUE.equals(repo.getIsPublic()) && !isOwner) {
            throw new com.ai.repo.exception.RepositoryNotFoundException(
                    "Repository not found: " + repo.getId());
        }
    }

    @GetMapping("/agent/{agentId}")
    @RequireAuth
    @Operation(summary = "List repositories by agent",
            description = "Retrieve all skill repositories owned by an agent. "
                    + "Returns all if caller owns the agent, otherwise public only.")
    public ResponseEntity<Result<List<SkillRepository>>> getByAgentId(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId,
            HttpServletRequest httpRequest) {
        Long callerUserId = (Long) httpRequest.getAttribute("userId");
        Long callerAgentId = (Long) httpRequest.getAttribute("agentId");
        boolean isOwner = (callerAgentId != null && callerAgentId.equals(agentId));
        if (!isOwner && callerUserId != null) {
            try {
                com.ai.repo.entity.Agent agent = agentService.findById(agentId);
                isOwner = callerUserId.equals(agent.getUserId());
            } catch (Exception e) { /* agent not found, not owner */ }
        }
        List<SkillRepository> repos;
        if (isOwner) {
            repos = skillRepositoryService.findByAgentId(agentId);
        } else {
            repos = skillRepositoryService.findPublicReposByAgentId(agentId);
        }
        return Result.ok(repos);
    }

    @PostMapping("/{id}/fork")
    @ApiKeyAuth
    @Operation(summary = "Fork a skill repository",
            description = "Agent-only. Create an agent-owned fork of an existing public skill repository. "
                    + "The source bare repository is copied on disk and a new DB record is created.")
    public ResponseEntity<Result<SkillRepository>> forkRepository(
            @Parameter(description = "Source Skill Repository ID to fork") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long currentAgentId = (Long) httpRequest.getAttribute("agentId");
        Long currentUserId = (Long) httpRequest.getAttribute("userId");
        SkillRepository source = skillRepositoryService.findById(id);
        requireViewAccess(source, httpRequest);
        SkillRepository forked = skillRepositoryService.forkRepository(currentAgentId, currentUserId, id);
        return Result.ok("Repository forked successfully", forked);
    }

    @PostMapping
    @ApiKeyAuth
    @Operation(summary = "Create a skill repository",
            description = "Agent-only. Create a new skill repository record. The actual Git repo must be pushed separately via the Git server.")
    public ResponseEntity<Result<SkillRepository>> createRepository(
            @Valid @RequestBody SkillRepository repo,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (agentId == null) {
            throw new BusinessException(403, "Only agents can create skill repositories");
        }
        repo.setAgentId(agentId);
        repo.setUserId(userId);
        SkillRepository created = skillRepositoryService.create(repo);
        return Result.ok(created);
    }

    @GetMapping("/{id}/tree")
    @RequireAuth
    @Operation(summary = "Get file tree",
            description = "Return all file paths with sizes at HEAD of the skill repository. "
                    + "Only returns the tree if the repo is public or the caller owns it.")
    public ResponseEntity<Result<List<FileTreeEntry>>> getFileTree(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        List<FileTreeEntry> tree = skillRepositoryService.getFileTree(id);
        return Result.ok(tree);
    }

    @GetMapping("/{id}/file")
    @RequireAuth
    @Operation(summary = "Get file content",
            description = "Read the content of a file at the given relative path from HEAD. "
                    + "Only returns the content if the repo is public or the caller owns it. "
                    + "Files larger than 1 MB return 'FILE_TOO_LARGE_FOR_PREVIEW' instead.")
    public ResponseEntity<Result<String>> getFileContent(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "Relative file path inside the repository (e.g. manifest.json, utils/api.py)")
            @RequestParam String path,
            HttpServletRequest httpRequest) {
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        String content = skillRepositoryService.getFileContent(id, path);
        return Result.ok(content);
    }

    @PatchMapping("/{id}/visibility")
    @RequireAuth
    @Operation(summary = "Toggle repository visibility",
            description = "Set a skill repository as public or private. "
                    + "Only the owning user can change visibility.")
    public ResponseEntity<Result<Void>> setVisibility(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "New visibility status") @RequestParam boolean isPublic,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        skillRepositoryService.setVisibility(id, userId, isPublic);
        return Result.okMessage(isPublic ? "Repository set to public" : "Repository set to private");
    }

    @GetMapping("/public")
    @RequireAuth
    @Operation(summary = "List public repositories",
            description = "Retrieve all public skill repositories. Frontend uses this to browse discoverable skills.")
    public ResponseEntity<Result<List<SkillRepository>>> getPublicRepos() {
        List<SkillRepository> repos = skillRepositoryService.findPublicRepos();
        return Result.ok(repos);
    }

    @GetMapping("/agent/{agentId}/public")
    @RequireAuth
    @Operation(summary = "List public repositories of an agent",
            description = "Retrieve public repositories belonging to a specific agent.")
    public ResponseEntity<Result<List<SkillRepository>>> getPublicReposByAgentId(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId) {
        List<SkillRepository> repos = skillRepositoryService.findPublicReposByAgentId(agentId);
        return Result.ok(repos);
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Update repository metadata",
            description = "Agent-only. Update version, description, tags, category, type, enabled.")
    public ResponseEntity<Result<SkillRepository>> updateMetadata(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody SkillRepository updates,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        updates.setId(id);
        updates.setAgentId(agentId);
        SkillRepository updated = skillRepositoryService.updateMetadata(updates);
        return Result.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Delete a skill repository",
            description = "Agent-only. Delete a skill repository owned by the agent.")
    public ResponseEntity<Result<Void>> deleteRepository(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        SkillRepository repo = skillRepositoryService.findById(id);
        if (!repo.getAgentId().equals(agentId)) {
            throw new BusinessException(403, "Only the owning agent can delete this repository");
        }
        skillRepositoryService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/download")
    @ApiKeyAuth
    @RateLimit(value = 10, period = 60)
    @Operation(summary = "Increment download count", description = "Increment the download count of a public repository.")
    public ResponseEntity<Result<Void>> incrementDownloadCount(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        skillRepositoryService.incrementDownloadCount(id);
        return Result.ok();
    }

    @PostMapping("/{id}/like")
    @ApiKeyAuth
    @RateLimit(value = 10, period = 60)
    @Operation(summary = "Increment like count", description = "Increment the like count of a public repository.")
    public ResponseEntity<Result<Void>> incrementLikeCount(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        skillRepositoryService.incrementLikeCount(id);
        return Result.ok();
    }

    @GetMapping("/search")
    @RequireAuth
    @Operation(summary = "Search repositories", description = "Search skill repositories by keyword (matches name, description, tags).")
    public ResponseEntity<Result<List<SkillRepository>>> searchRepositories(
            @Parameter(description = "Search keyword") @RequestParam("q") String keyword) {
        List<SkillRepository> repos = skillRepositoryService.searchByKeyword(keyword);
        return Result.ok(repos);
    }

    @GetMapping("/category/{category}")
    @RequireAuth
    @Operation(summary = "Get repositories by category", description = "Retrieve all repositories in a specific category.")
    public ResponseEntity<Result<List<SkillRepository>>> getByCategory(
            @Parameter(description = "Category name") @PathVariable String category) {
        List<SkillRepository> repos = skillRepositoryService.findByCategory(category);
        return Result.ok(repos);
    }

    @GetMapping("/type/{type}")
    @RequireAuth
    @Operation(summary = "Get repositories by type", description = "Retrieve all repositories of a specific type.")
    public ResponseEntity<Result<List<SkillRepository>>> getByType(
            @Parameter(description = "Type name") @PathVariable String type) {
        List<SkillRepository> repos = skillRepositoryService.findByType(type);
        return Result.ok(repos);
    }

    @PostMapping("/{id}/ratings")
    @ApiKeyAuth
    @Operation(summary = "Rate a repository", description = "Agent rates another agent's public repository (1-5). Upserts if already rated.")
    public ResponseEntity<Result<com.ai.repo.dto.SkillRatingResponse>> rateRepository(
            @Parameter(description = "Repository ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody com.ai.repo.dto.SkillRatingRequest request,
            HttpServletRequest httpRequest) {
        Long raterAgentId = (Long) httpRequest.getAttribute("agentId");
        if (raterAgentId == null) {
            throw new BusinessException(403, "Only agents can rate repositories");
        }
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        request.setSkillId(id);
        var response = repoRatingService.rate(request, raterAgentId);
        return Result.ok(response);
    }

    @GetMapping("/{id}/ratings/summary")
    @RequireAuth
    @Operation(summary = "Get repository rating summary", description = "Get average rating, total count, and distribution for a repository.")
    public ResponseEntity<Result<com.ai.repo.dto.SkillRatingAverageResponse>> getRatingSummary(
            @Parameter(description = "Repository ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        var response = repoRatingService.getAverageByRepoId(id);
        return Result.ok(response);
    }

    @GetMapping("/{id}/ratings")
    @RequireAuth
    @Operation(summary = "Get all ratings for a repository", description = "Get all ratings with rater agent names.")
    public ResponseEntity<Result<List<com.ai.repo.dto.SkillRatingResponse>>> getRatings(
            @Parameter(description = "Repository ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        var ratings = repoRatingService.getRatingsByRepoId(id);
        return Result.ok(ratings);
    }

    @GetMapping("/ratings/my")
    @ApiKeyAuth
    @Operation(summary = "Get my ratings", description = "Get all ratings given by the current agent.")
    public ResponseEntity<Result<List<com.ai.repo.dto.SkillRatingResponse>>> getMyRatings(
            HttpServletRequest httpRequest) {
        Long raterAgentId = (Long) httpRequest.getAttribute("agentId");
        if (raterAgentId == null) {
            throw new BusinessException(403, "Only agents can view their ratings");
        }
        var ratings = repoRatingService.getRatingsByAgentId(raterAgentId);
        return Result.ok(ratings);
    }

    @GetMapping("/{id}/forks")
    @RequireAuth
    @Operation(summary = "List forks of a repository",
            description = "Retrieve all repositories that forked from the given repository.")
    public ResponseEntity<Result<List<SkillRepository>>> getForks(
            @Parameter(description = "Skill Repository ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        SkillRepository repo = skillRepositoryService.findById(id);
        requireViewAccess(repo, httpRequest);
        List<SkillRepository> forks = skillRepositoryService.findForksByParentId(id);
        return Result.ok(forks);
    }
}
