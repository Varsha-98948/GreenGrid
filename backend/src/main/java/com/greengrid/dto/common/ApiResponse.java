package com.greengrid.dto.common;

/**
 * Uniform success envelope. Kept intentionally simple (no nested "meta"
 * object) — pagination info travels on Spring's own Page<T> serialization
 * for endpoints that need it, this wrapper is for single-resource /
 * action-result responses.
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
