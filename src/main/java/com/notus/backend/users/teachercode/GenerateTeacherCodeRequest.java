package com.notus.backend.users.teachercode;

import java.time.Instant;

public record GenerateTeacherCodeRequest(
        String code,
        Instant expiresAt,
        Integer usageLimit
) {
}
