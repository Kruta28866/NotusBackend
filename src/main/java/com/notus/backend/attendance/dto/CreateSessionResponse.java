package com.notus.backend.attendance.dto;

import java.time.Instant;

public record CreateSessionResponse(
        Long sessionId,
        String scheduleId,
        String title,
        String room,
        String time,
        Instant createdAt,
        boolean active
) {}