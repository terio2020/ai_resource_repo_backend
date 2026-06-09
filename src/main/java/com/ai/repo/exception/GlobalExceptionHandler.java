package com.ai.repo.exception;

import com.ai.repo.common.Result;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import org.eclipse.jgit.api.errors.GitAPIException;
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
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("Business exception: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<?> handleAuthenticationException(AuthenticationException e) {
        log.error("Authentication exception: {}", e.getMessage());
        return Result.error(401, e.getMessage());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public Result<?> handleTokenExpiredException(TokenExpiredException e) {
        log.warn("Token expired: {}", e.getMessage());
        return Result.error(401, "Token expired, please refresh");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return Result.error(403, "Access denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
        log.error("Validation exception: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Binding failed";
        log.error("Bind exception: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParams(MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getMessage());
        return Result.error(400, "Required parameter '" + e.getParameterName() + "' is missing");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("System exception: ", e);
        return Result.error("System error, please contact administrator");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public Result<?> handleInvalidFileTypeException(InvalidFileTypeException e) {
        log.warn("Invalid file type: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(FileTooLargeException.class)
    public Result<?> handleFileTooLargeException(FileTooLargeException e) {
        log.warn("File too large: {}", e.getMessage());
        return Result.error(413, e.getMessage());
    }

    @ExceptionHandler(FileStorageException.class)
    public Result<?> handleFileStorageException(FileStorageException e) {
        log.error("File storage exception: {}", e.getMessage(), e);
        return Result.error(500, "File storage failed: " + e.getMessage());
    }

    @ExceptionHandler(RepositoryNotFoundException.class)
    public Result<?> handleRepositoryNotFoundException(RepositoryNotFoundException e) {
        log.warn("Repository not found: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(FileNotAllowedException.class)
    public Result<?> handleFileNotAllowedException(FileNotAllowedException e) {
        log.warn("File not allowed: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public Result<?> handleIOException(IOException e) {
        log.error("IO exception: {}", e.getMessage(), e);
        return Result.error(500, "Internal I/O error: " + e.getMessage());
    }

    @ExceptionHandler(GitAPIException.class)
    public Result<?> handleGitAPIException(GitAPIException e) {
        log.error("Git operation failed: {}", e.getMessage(), e);
        return Result.error(500, "Git operation failed: " + e.getMessage());
    }
}
