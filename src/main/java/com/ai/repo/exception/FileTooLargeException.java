package com.ai.repo.exception;

public class FileTooLargeException extends RuntimeException {
    private long maxSize;

    public FileTooLargeException(String message, long maxSize) {
        super(message);
        this.maxSize = maxSize;
    }

    public long getMaxSize() {
        return maxSize;
    }
}