package com.eventplatform.config;

import com.eventplatform.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class WebExceptionAdvice {
    private static final Logger log=LoggerFactory.getLogger(WebExceptionAdvice.class);
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Result.fail(e.getReason()));
    }
    @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Result> invalid(Exception e) {
        return ResponseEntity.badRequest().body(Result.fail("Invalid request parameters"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result> upload(Exception e) {
        return ResponseEntity.status(413).body(Result.fail("Image size exceeds the limit"));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result> unexpected(RuntimeException e) {
        log.error("Request failed", e);
        return ResponseEntity.internalServerError().body(Result.fail("Internal server error"));
    }
}
