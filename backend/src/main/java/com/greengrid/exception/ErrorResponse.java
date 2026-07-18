package com.greengrid.exception;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error shape returned to the frontend for every failure case,
 * so client-side error handling never has to branch on which endpoint failed.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> fieldErrors
) {
    public record FieldViolation(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, List.of());
    }

    public static ErrorResponse ofFieldErrors(String message, String path, List<FieldViolation> violations) {
        return new ErrorResponse(Instant.now(), 400, "Validation Failed", message, path, violations);
    }
}
