package com.ai.repo.exception;

import lombok.Getter;

@Getter
public class FileNotAllowedException extends RuntimeException {
    private final Integer code;

    public FileNotAllowedException(String message) {
        super(message);
        this.code = 400;
    }
}
