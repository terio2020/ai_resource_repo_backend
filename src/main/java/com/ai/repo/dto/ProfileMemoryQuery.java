package com.ai.repo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProfileMemoryQuery {
    private List<String> namespaces;
    private Integer maxItems;

    public static ProfileMemoryQuery empty() {
        return new ProfileMemoryQuery(null, null);
    }
}
