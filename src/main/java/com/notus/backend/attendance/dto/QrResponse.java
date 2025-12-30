package com.notus.backend.attendance.dto;

public record QrResponse(
        Long sessionId,
        String qrToken,
        String qrPngBase64,
        long expiresAtEpochSeconds
) {}
