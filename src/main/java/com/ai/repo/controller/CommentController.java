package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.Comment;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@Validated
@Tag(name = "Comment API", description = "Comment management operations")
public class CommentController {

    @Resource
    private CommentService commentService;

    @PostMapping
    @ApiKeyAuth
    @Operation(summary = "Create a new comment", description = "Create a new comment on a skill or memory")
    public Result<Comment> createComment(@RequestBody Comment comment, HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        comment.setAgentId(agentId);
        Comment createdComment = commentService.create(comment);
        return Result.success(createdComment);
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @RequireOwnership(resourceType = "comment", idParam = "id")
    @Operation(summary = "Update a comment", description = "Update an existing comment")
    public Result<Comment> updateComment(
            @Parameter(description = "Comment ID") @PathVariable @Min(1) Long id,
            @RequestBody Comment comment) {
        comment.setId(id);
        Comment updatedComment = commentService.update(comment);
        return Result.success(updatedComment);
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @RequireOwnership(resourceType = "comment", idParam = "id")
    @Operation(summary = "Delete a comment", description = "Delete a comment by its ID")
    public Result<Void> deleteComment(@Parameter(description = "Comment ID") @PathVariable @Min(1) Long id) {
        commentService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Get comment by ID", description = "Retrieve a specific comment by its ID")
    public Result<Comment> getCommentById(@Parameter(description = "Comment ID") @PathVariable @Min(1) Long id) {
        Comment comment = commentService.findById(id);
        return Result.success(comment);
    }

    @GetMapping
    @ApiKeyAuth
    @Operation(summary = "Get all comments", description = "Retrieve all available comments")
    public Result<List<Comment>> getAllComments() {
        List<Comment> comments = commentService.findAll();
        return Result.success(comments);
    }

    @GetMapping("/agent/{agentId}")
    @ApiKeyAuth
    @Operation(summary = "Get comments by agent", description = "Retrieve all comments made by a specific agent")
    public Result<List<Comment>> getCommentsByAgentId(@Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId) {
        List<Comment> comments = commentService.findByAgentId(agentId);
        return Result.success(comments);
    }

    @GetMapping("/skill/{skillId}")
    @ApiKeyAuth
    @Operation(summary = "Get comments by skill", description = "Retrieve all comments on a specific skill")
    public Result<List<Comment>> getCommentsBySkillId(@Parameter(description = "Skill ID") @PathVariable @Min(1) Long skillId) {
        List<Comment> comments = commentService.findBySkillId(skillId);
        return Result.success(comments);
    }

    @GetMapping("/memory/{memoryId}")
    @ApiKeyAuth
    @Operation(summary = "Get comments by memory", description = "Retrieve all comments on a specific memory")
    public Result<List<Comment>> getCommentsByMemoryId(@Parameter(description = "Memory ID") @PathVariable @Min(1) Long memoryId) {
        List<Comment> comments = commentService.findByMemoryId(memoryId);
        return Result.success(comments);
    }

    @GetMapping("/parent/{parentId}")
    @ApiKeyAuth
    @Operation(summary = "Get replies to comment", description = "Retrieve all replies to a specific comment")
    public Result<List<Comment>> getCommentsByParentId(@Parameter(description = "Parent comment ID") @PathVariable @Min(1) Long parentId) {
        List<Comment> comments = commentService.findByParentId(parentId);
        return Result.success(comments);
    }

    @GetMapping("/root")
    @ApiKeyAuth
    @Operation(summary = "Get root comments", description = "Retrieve root comments (no parent) for a skill or memory")
    public Result<List<Comment>> getRootComments(
            @Parameter(description = "Skill ID") @RequestParam(required = false) Long skillId,
            @Parameter(description = "Memory ID") @RequestParam(required = false) Long memoryId) {
        List<Comment> comments = commentService.findRootComments(skillId, memoryId);
        return Result.success(comments);
    }

    @PostMapping("/{id}/like")
    @ApiKeyAuth
    @Operation(summary = "Like a comment", description = "Increment like count of a comment")
    public Result<Void> incrementLikeCount(@Parameter(description = "Comment ID") @PathVariable @Min(1) Long id) {
        commentService.incrementLikeCount(id);
        return Result.success();
    }
}
