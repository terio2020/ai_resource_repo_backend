package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.ChatMessage;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat API", description = "Chat message operations")
public class ChatMessageController {

    @Resource
    private ChatMessageService chatMessageService;

    @PostMapping
    @RequireAuth
    @Operation(summary = "Create a chat message", description = "Create a new chat message in a room")
    public Result<ChatMessage> createMessage(@RequestBody ChatMessage message, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        ChatMessage createdMessage = chatMessageService.create(message);
        return Result.success(createdMessage);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "chatMessage", idParam = "id")
    @Operation(summary = "Delete a chat message", description = "Delete a chat message by its ID")
    public Result<Void> deleteMessage(@Parameter(description = "Message ID") @PathVariable Long id) {
        chatMessageService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get chat message by ID", description = "Retrieve a specific chat message by its ID")
    public Result<ChatMessage> getMessageById(@Parameter(description = "Message ID") @PathVariable Long id) {
        ChatMessage message = chatMessageService.findById(id);
        return Result.success(message);
    }

    @GetMapping
    @Operation(summary = "Get all chat messages", description = "Retrieve all available chat messages")
    public Result<List<ChatMessage>> getAllMessages() {
        List<ChatMessage> messages = chatMessageService.findAll();
        return Result.success(messages);
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get messages by room", description = "Retrieve all messages in a specific chat room")
    public Result<List<ChatMessage>> getMessagesByRoomId(@Parameter(description = "Room ID") @PathVariable String roomId) {
        List<ChatMessage> messages = chatMessageService.findByRoomId(roomId);
        return Result.success(messages);
    }

    @GetMapping("/sender/{senderId}")
    @Operation(summary = "Get messages by sender", description = "Retrieve all messages sent by a specific sender")
    public Result<List<ChatMessage>> getMessagesBySenderId(@Parameter(description = "Sender ID") @PathVariable Long senderId) {
        List<ChatMessage> messages = chatMessageService.findBySenderId(senderId);
        return Result.success(messages);
    }

    @GetMapping("/room/{roomId}/recent")
    @Operation(summary = "Get recent messages", description = "Retrieve recent messages in a chat room with limit")
    public Result<List<ChatMessage>> getRecentMessages(
            @Parameter(description = "Room ID") @PathVariable String roomId,
            @Parameter(description = "Limit number of results") @RequestParam(defaultValue = "50") int limit) {
        List<ChatMessage> messages = chatMessageService.findRecentMessages(roomId, limit);
        return Result.success(messages);
    }
}
