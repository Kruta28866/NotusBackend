package com.notus.backend.auth.dto;

public record TeacherVerifyCodeRequest(
        String code,
        String email
) {
}
