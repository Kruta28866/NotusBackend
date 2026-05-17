package com.notus.backend.activity.dto;

import java.time.Instant;

public record TeacherNotificationResponse(
        String id,
        String type,
        String title,
        String body,
        String severity,
        boolean read,
        Instant createdAt,
        String actionUrl
) {}
