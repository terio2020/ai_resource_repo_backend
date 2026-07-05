package com.ai.repo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return new Result<>(200, "Success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "Success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public HttpStatus getHttpStatus() {
        if (code == null) {
            return HttpStatus.OK;
        }
        HttpStatus status = HttpStatus.resolve(code);
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public static <T> ResponseEntity<Result<T>> ok(T data) {
        return ResponseEntity.ok(Result.success(data));
    }

    public static ResponseEntity<Result<Void>> ok() {
        return ResponseEntity.ok(Result.<Void>success());
    }

    public static ResponseEntity<Result<Void>> okMessage(String message) {
        return ResponseEntity.ok(Result.<Void>success(message));
    }

    public static <T> ResponseEntity<Result<T>> ok(String message, T data) {
        return ResponseEntity.ok(Result.success(message, data));
    }

    public static <T> ResponseEntity<Result<T>> fail(Integer code, String message) {
        Result<T> result = Result.error(code, message);
        return ResponseEntity.status(result.getHttpStatus()).body(result);
    }

    public static <T> ResponseEntity<Result<T>> fail(String message) {
        Result<T> result = Result.error(message);
        return ResponseEntity.status(result.getHttpStatus()).body(result);
    }

    public static ResponseEntity<Result<?>> failRaw(Integer code, String message) {
        Result<?> result = Result.error(code, message);
        return ResponseEntity.status(result.getHttpStatus()).body(result);
    }

    public static ResponseEntity<Result<?>> failRaw(String message) {
        Result<?> result = Result.error(message);
        return ResponseEntity.status(result.getHttpStatus()).body(result);
    }
}