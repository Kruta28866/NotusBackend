package com.notus.backend.auth.dto;

public record TeacherGoogleRegisterRequest(
        String idToken,
        String registrationToken
) {}
