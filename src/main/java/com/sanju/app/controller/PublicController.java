package com.sanju.app.controller;

import com.sanju.app.api.ApiResponse;
import com.sanju.app.api.InfoResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PublicController {

    private final BuildProperties buildProperties;

    public PublicController(ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.buildProperties = buildPropertiesProvider.getIfAvailable();
    }

    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<Map<String, String>>> hello(
            @RequestParam(defaultValue = "World") String name
    ) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Hello, " + name + "!")));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("service", "app", "status", "UP")));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<InfoResponse>> info() {
        String version = buildProperties != null ? buildProperties.getVersion() : "unknown";
        String buildTime = (buildProperties != null && buildProperties.getTime() != null)
                ? buildProperties.getTime().toString()
                : "unknown";

        InfoResponse payload = new InfoResponse("app", version, buildTime);
        return ResponseEntity.ok(ApiResponse.ok(payload));
    }
}