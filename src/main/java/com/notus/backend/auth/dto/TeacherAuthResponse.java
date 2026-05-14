package com.notus.backend.auth.dto;

import com.notus.backend.users.UserDto;

public record TeacherAuthResponse(
        String token,
        UserDto user,
        boolean emailVerified,
        boolean requiresEmailVerification,
        String message
) {}
