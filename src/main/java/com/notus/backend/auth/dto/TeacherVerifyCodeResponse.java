package com.notus.backend.auth.dto;

public record TeacherVerifyCodeResponse(
        boolean valid,
        String registrationToken
) {}
