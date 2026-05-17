package com.notus.backend.teachergroups.dto;

import java.time.Instant;

public record StudentGroupResponse(
        Long id,
        String name,
        String description,
        String subject,
        String schoolYear,
        String semester,
        String teacherName,
        String teacherEmail,
        Instant joinedAt
) {}
