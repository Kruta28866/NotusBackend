package com.notus.backend.activity.dto;

import java.time.Instant;

public record TeacherActivityItemResponse(
        String id,
        String type,
        String title,
        String description,
        String icon,
        Instant occurredAt,
        String actionUrl
) {}
