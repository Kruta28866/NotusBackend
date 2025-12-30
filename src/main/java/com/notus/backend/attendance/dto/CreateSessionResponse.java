package com.notus.backend.attendance.dto;

import java.time.Instant;

public record CreateSessionResponse(Long sessionId, String title, Instant createdAt, boolean active) {}
