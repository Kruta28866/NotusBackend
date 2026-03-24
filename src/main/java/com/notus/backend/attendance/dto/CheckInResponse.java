package com.notus.backend.attendance.dto;

import java.time.Instant;

public record CheckInResponse(
        Long sessionId,
        String sessionTitle,
        String studentUid,
        String studentName,
        String indexNumber,
        Instant checkedInAt,

        boolean alreadyCheckIn,
        Instant sessionEndsAt
) {}