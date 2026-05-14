package com.notus.backend.users.teachercode;

import java.time.Instant;

public record TeacherCodeDto(
        Long id,
        String code,
        boolean isActive,
        Instant createdAt,
        Instant expiresAt,
        Integer usageLimit,
        int timesUsed
) {
    public static TeacherCodeDto from(TeacherCode teacherCode) {
        return new TeacherCodeDto(
                teacherCode.getId(),
                teacherCode.getCode(),
                teacherCode.isActive(),
                teacherCode.getCreatedAt(),
                teacherCode.getExpiresAt(),
                teacherCode.getUsageLimit(),
                teacherCode.getTimesUsed()
        );
    }
}
