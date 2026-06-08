package com.codezilla.crm.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Maps low-level integration failures (Meta, OpenAI, Telegram, etc.) to a
 * meaningful HTTP status so callers see "the upstream service rejected this"
 * (502) rather than an opaque 500 stack trace.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> upstreamRejected(WebClientResponseException ex) {
        log.warn("Upstream API rejected request: status={} body={}",
                ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "upstream_error",
                "upstreamStatus", ex.getStatusCode().value(),
                "message", ex.getResponseBodyAsString()));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> upstreamUnreachable(WebClientRequestException ex) {
        log.warn("Upstream API unreachable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "upstream_unreachable",
                "message", ex.getMessage()));
    }
}
