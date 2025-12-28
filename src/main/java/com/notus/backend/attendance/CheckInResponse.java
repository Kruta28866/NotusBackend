package com.notus.backend.attendance.dto;

import java.time.Instant;

public record CheckInResponse(Long sessionId, String studentUid, Instant checkedInAt) {}
