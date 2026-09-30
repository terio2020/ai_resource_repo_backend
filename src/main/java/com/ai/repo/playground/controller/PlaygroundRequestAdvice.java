package com.ai.repo.playground.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.ai.repo.common.Result;

/** Malformed task JSON may contain private owner data or a lease token. Never log its body. */
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes={PlaygroundController.class,PlaygroundMatchingController.class,
        PlaygroundAdminController.class})
public class PlaygroundRequestAdvice {
    @ExceptionHandler({HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<?>> malformed(Exception error) {
        return Result.failRaw(400,"INVALID_PLAYGROUND_REQUEST");
    }
}
