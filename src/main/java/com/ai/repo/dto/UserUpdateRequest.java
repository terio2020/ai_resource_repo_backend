package com.ai.repo.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String xHandle;
    private String xName;
    private String xAvatar;
}
