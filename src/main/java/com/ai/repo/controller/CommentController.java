package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.Comment;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.CommentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Resource
    private CommentService commentService;

    @PostMapping
    @RequireAuth
    public Result<Comment> createComment(@RequestBody Comment comment, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        comment.setUserId(userId);
        Comment createdComment = commentService.create(comment);
        return Result.success(createdComment);
    }

    @PutMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "comment", idParam = "id")
    public Result<Comment> updateComment(@PathVariable Long id, @RequestBody Comment comment) {
        comment.setId(id);
        Comment updatedComment = commentService.update(comment);
        return Result.success(updatedComment);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "comment", idParam = "id")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Comment> getCommentById(@PathVariable Long id) {
        Comment comment = commentService.findById(id);
        return Result.success(comment);
    }

    @GetMapping
    public Result<List<Comment>> getAllComments() {
        List<Comment> comments = commentService.findAll();
        return Result.success(comments);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Comment>> getCommentsByUserId(@PathVariable Long userId) {
        List<Comment> comments = commentService.findByUserId(userId);
        return Result.success(comments);
    }

    @GetMapping("/skill/{skillId}")
    public Result<List<Comment>> getCommentsBySkillId(@PathVariable Long skillId) {
        List<Comment> comments = commentService.findBySkillId(skillId);
        return Result.success(comments);
    }

    @GetMapping("/memory/{memoryId}")
    public Result<List<Comment>> getCommentsByMemoryId(@PathVariable Long memoryId) {
        List<Comment> comments = commentService.findByMemoryId(memoryId);
        return Result.success(comments);
    }

    @GetMapping("/parent/{parentId}")
    public Result<List<Comment>> getCommentsByParentId(@PathVariable Long parentId) {
        List<Comment> comments = commentService.findByParentId(parentId);
        return Result.success(comments);
    }

    @GetMapping("/root")
    public Result<List<Comment>> getRootComments(
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) Long memoryId) {
        List<Comment> comments = commentService.findRootComments(skillId, memoryId);
        return Result.success(comments);
    }

    @PostMapping("/{id}/like")
    public Result<Void> incrementLikeCount(@PathVariable Long id) {
        commentService.incrementLikeCount(id);
        return Result.success();
    }
}
