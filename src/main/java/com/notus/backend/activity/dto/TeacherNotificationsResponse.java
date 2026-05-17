package com.notus.backend.activity.dto;

import java.time.Instant;
import java.util.List;

public record TeacherNotificationsResponse(
        List<TeacherNotificationResponse> notifications,
        long unreadCount,
        Instant generatedAt
) {}
