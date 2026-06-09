package com.ai.repo.exception;

import lombok.Getter;

@Getter
public class RepositoryNotFoundException extends RuntimeException {
    private final Integer code;

    public RepositoryNotFoundException(String message) {
        super(message);
        this.code = 404;
    }

    public RepositoryNotFoundException(Long repoId) {
        super("Repository not found: " + repoId);
        this.code = 404;
    }
}
