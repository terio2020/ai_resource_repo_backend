package com.ai.repo.security;

import jakarta.servlet.http.HttpServletRequest;

import com.ai.repo.exception.BusinessException;

public final class AgentMutationPolicy {
    public static final String HEADER = "X-Logicoma-Policy-Version";
    public static final String CURRENT_VERSION = "2026-09-10";

    private AgentMutationPolicy() {
    }

    public static void requireCurrent(HttpServletRequest request) {
        String supplied = request.getHeader(HEADER);
        if (!CURRENT_VERSION.equals(supplied)) {
            throw new BusinessException(428,
                    "Current Agent upload policy acknowledgement is required; send "
                            + HEADER + ": " + CURRENT_VERSION);
        }
    }
}
