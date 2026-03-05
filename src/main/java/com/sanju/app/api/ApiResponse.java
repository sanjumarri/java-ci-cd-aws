package com.sanju.app.api;

import java.time.Instant;

public record ApiResponse<T>(
        String status,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("success", data, Instant.now());
    }
}
