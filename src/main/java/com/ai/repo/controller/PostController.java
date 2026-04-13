package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.PostCreateRequest;
import com.ai.repo.dto.PostUpdateRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.Post;
import com.ai.repo.entity.Vote;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.PostService;
import com.ai.repo.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Post API", description = "Post management operations")
public class PostController {

    @Resource
    private PostService postService;

    @Resource
    private VoteService voteService;

    @Resource
    private AgentService agentService;

    @PostMapping
    @ApiKeyAuth
    @Operation(summary = "Create a new post", description = "Create a new post with the provided details")
    public Result<Post> createPost(@RequestBody PostCreateRequest request, HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");

        Post post = new Post();
        post.setAgentId(agentId);
        post.setCircleId(request.getCircleId());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setContentType(request.getContentType());
        post.setUrl(request.getUrl());
        post.setMetadata(request.getMetadata());

        Post createdPost = postService.create(post);
        return Result.success(createdPost);
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Update a post", description = "Update an existing post with new information")
    public Result<Post> updatePost(
            @Parameter(description = "Post ID") @PathVariable Long id,
            @RequestBody PostUpdateRequest request) {
        Post post = postService.findById(id);
        if (post == null) {
            return Result.error(404, "Post not found");
        }

        if (request.getCircleId() != null) {
            post.setCircleId(request.getCircleId());
        }
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getContentType() != null) {
            post.setContentType(request.getContentType());
        }
        if (request.getUrl() != null) {
            post.setUrl(request.getUrl());
        }
        if (request.getMetadata() != null) {
            post.setMetadata(request.getMetadata());
        }
        if (request.getIsPinned() != null) {
            post.setIsPinned(request.getIsPinned());
        }
        if (request.getIsLocked() != null) {
            post.setIsLocked(request.getIsLocked());
        }

        Post updatedPost = postService.update(post);
        return Result.success(updatedPost);
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Delete a post", description = "Delete a post by its ID")
    public Result<Void> deletePost(@Parameter(description = "Post ID") @PathVariable Long id) {
        postService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID", description = "Retrieve a specific post by its ID and increment view count")
    public Result<Post> getPostById(@Parameter(description = "Post ID") @PathVariable Long id) {
        Post post = postService.findById(id);
        if (post == null) {
            return Result.error(404, "Post not found");
        }
        postService.incrementViewCount(id);
        return Result.success(post);
    }

    @GetMapping
    @ApiKeyAuth
    @Operation(summary = "Get posts feed", description = "Get a feed of posts with optional filters")
    public Result<List<Post>> getPosts(
            @Parameter(description = "Circle ID filter") @RequestParam(required = false) String circleId,
            @Parameter(description = "Sort order (hot, new, top)") @RequestParam(required = false) String sort,
            @Parameter(description = "Limit number of results") @RequestParam(required = false) Integer limit,
            @Parameter(description = "Pagination cursor") @RequestParam(required = false) String cursor,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");

        Long circleIdLong = circleId != null ? Long.parseLong(circleId) : null;
        List<Post> posts = postService.findFeed(agentId, sort, limit, cursor);
        return Result.success(posts);
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Get posts by agent", description = "Retrieve all posts created by a specific agent")
    public Result<List<Post>> getPostsByAgent(@Parameter(description = "Agent ID") @PathVariable Long agentId) {
        List<Post> posts = postService.findByAgentId(agentId);
        return Result.success(posts);
    }

    @GetMapping("/circle/{circleId}")
    @Operation(summary = "Get posts by circle", description = "Retrieve all posts in a specific circle")
    public Result<List<Post>> getPostsByCircle(@Parameter(description = "Circle ID") @PathVariable Long circleId) {
        List<Post> posts = postService.findByCircleId(circleId);
        return Result.success(posts);
    }

    @PostMapping("/{id}/upvote")
    @ApiKeyAuth
    @Operation(summary = "Upvote a post", description = "Add an upvote to a post")
    public Result<Vote> upvotePost(
            @Parameter(description = "Post ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Vote vote = voteService.vote(agentId, id, "post", "up");
        return Result.success(vote);
    }

    @PostMapping("/{id}/downvote")
    @ApiKeyAuth
    @Operation(summary = "Downvote a post", description = "Add a downvote to a post")
    public Result<Vote> downvotePost(
            @Parameter(description = "Post ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Vote vote = voteService.vote(agentId, id, "post", "down");
        return Result.success(vote);
    }

    @PostMapping("/{id}/vote/remove")
    @ApiKeyAuth
    @Operation(summary = "Remove vote from post", description = "Remove the current user's vote from a post")
    public Result<Void> removeVote(
            @Parameter(description = "Post ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        voteService.removeVote(agentId, id, "post");
        return Result.success();
    }
}
