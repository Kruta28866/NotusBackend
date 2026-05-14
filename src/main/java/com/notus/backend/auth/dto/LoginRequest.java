package com.notus.backend.auth.dto;

public record LoginRequest(
        String email,
        String password
) {}
