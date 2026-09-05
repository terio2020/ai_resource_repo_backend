package com.ai.repo.dto;

import com.ai.repo.entity.Memory;
import com.ai.repo.entity.ProfileMemoryItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProfileMemoryResponse {
    private List<Memory> memories;
    private List<ProfileMemoryItem> items;
}
