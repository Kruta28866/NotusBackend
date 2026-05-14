package com.notus.backend.auth.dto;

public record StudentRegisterRequest(
        String name,
        String email,
        String password
) {}
