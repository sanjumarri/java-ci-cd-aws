package com.sanju.app.api;

public record InfoResponse(
        String appName,
        String version,
        String buildTime
) {}
