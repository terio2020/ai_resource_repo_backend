package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserResponse {
    private Long id;
    private String uid;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
