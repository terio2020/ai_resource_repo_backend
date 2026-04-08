package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.ChatMessage;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.ChatMessageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatMessageController {

    @Resource
    private ChatMessageService chatMessageService;

    @PostMapping
    @RequireAuth
    public Result<ChatMessage> createMessage(@RequestBody ChatMessage message, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        ChatMessage createdMessage = chatMessageService.create(message);
        return Result.success(createdMessage);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "chatMessage", idParam = "id")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        chatMessageService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ChatMessage> getMessageById(@PathVariable Long id) {
        ChatMessage message = chatMessageService.findById(id);
        return Result.success(message);
    }

    @GetMapping
    public Result<List<ChatMessage>> getAllMessages() {
        List<ChatMessage> messages = chatMessageService.findAll();
        return Result.success(messages);
    }

    @GetMapping("/room/{roomId}")
    public Result<List<ChatMessage>> getMessagesByRoomId(@PathVariable String roomId) {
        List<ChatMessage> messages = chatMessageService.findByRoomId(roomId);
        return Result.success(messages);
    }

    @GetMapping("/sender/{senderId}")
    public Result<List<ChatMessage>> getMessagesBySenderId(@PathVariable Long senderId) {
        List<ChatMessage> messages = chatMessageService.findBySenderId(senderId);
        return Result.success(messages);
    }

    @GetMapping("/room/{roomId}/recent")
    public Result<List<ChatMessage>> getRecentMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit) {
        List<ChatMessage> messages = chatMessageService.findRecentMessages(roomId, limit);
        return Result.success(messages);
    }
}
