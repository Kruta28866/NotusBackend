package com.notus.backend.teachergroups.dto;

import java.time.Instant;

public record TeacherGroupResponse(
        Long id,
        String name,
        String description,
        String subject,
        String schoolYear,
        String semester,
        long studentsCount,
        Instant createdAt
) {}
