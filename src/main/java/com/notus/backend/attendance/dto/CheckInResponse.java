package com.notus.backend.attendance.dto;

import java.time.Instant;

public record CheckInResponse(Long sessionId, String studentUid, String studentName, String studentIndex, Instant checkedInAt) {}
