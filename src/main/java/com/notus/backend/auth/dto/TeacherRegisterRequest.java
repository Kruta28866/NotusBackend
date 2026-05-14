package com.notus.backend.auth.dto;

public record TeacherRegisterRequest(
        String registrationToken,
        String name,
        String email,
        String password
) {}
