package com.ai.repo.exception;

import com.ai.repo.common.Result;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        log.error("Business exception: {}", e.getMessage());
        return Result.failRaw(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<?>> handleAuthenticationException(AuthenticationException e) {
        log.error("Authentication exception: {}", e.getMessage());
        return Result.failRaw(401, e.getMessage());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Result<?>> handleTokenExpiredException(TokenExpiredException e) {
        log.warn("Token expired: {}", e.getMessage());
        return Result.failRaw(401, "Token expired, please refresh");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return Result.failRaw(403, "Access denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
        log.error("Validation exception: {}", message);
        return Result.failRaw(400, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<?>> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Binding failed";
        log.error("Bind exception: {}", message);
        return Result.failRaw(400, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<?>> handleMissingParams(MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getMessage());
        return Result.failRaw(400, "Required parameter '" + e.getParameterName() + "' is missing");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<?>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Validation failed");
        log.warn("Constraint violation: {}", message);
        return Result.failRaw(400, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        log.error("System exception: ", e);
        return Result.failRaw("System error, please contact administrator");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return Result.failRaw(400, e.getMessage());
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Result<?>> handleInvalidFileTypeException(InvalidFileTypeException e) {
        log.warn("Invalid file type: {}", e.getMessage());
        return Result.failRaw(400, e.getMessage());
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<Result<?>> handleFileTooLargeException(FileTooLargeException e) {
        log.warn("File too large: {}", e.getMessage());
        return Result.failRaw(413, e.getMessage());
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Result<?>> handleFileStorageException(FileStorageException e) {
        log.error("File storage exception: {}", e.getMessage(), e);
        return Result.failRaw(500, "File storage failed: " + e.getMessage());
    }

    @ExceptionHandler(RepositoryNotFoundException.class)
    public ResponseEntity<Result<?>> handleRepositoryNotFoundException(RepositoryNotFoundException e) {
        log.warn("Repository not found: {}", e.getMessage());
        return Result.failRaw(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(FileNotAllowedException.class)
    public ResponseEntity<Result<?>> handleFileNotAllowedException(FileNotAllowedException e) {
        log.warn("File not allowed: {}", e.getMessage());
        return Result.failRaw(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Result<?>> handleIOException(IOException e) {
        log.error("IO exception: {}", e.getMessage(), e);
        return Result.failRaw(500, "Internal I/O error: " + e.getMessage());
    }

    @ExceptionHandler(GitAPIException.class)
    public ResponseEntity<Result<?>> handleGitAPIException(GitAPIException e) {
        log.error("Git operation failed: {}", e.getMessage(), e);
        return Result.failRaw(500, "Git operation failed: " + e.getMessage());
    }
}