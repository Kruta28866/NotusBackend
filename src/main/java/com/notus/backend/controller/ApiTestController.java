package com.notus.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiTestController {


    @GetMapping("/api/test")
    public Map<String, Object> test() {
        return Map.of(
                "message", "Backend działa",
                "status", "OK"
        );
    }

    @GetMapping({"/api/health", "/actuator/health"})
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
