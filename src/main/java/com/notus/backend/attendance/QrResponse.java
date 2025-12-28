package com.notus.backend.attendance;

public record QrResponse(
        Long sessionId,
        String qrToken,
        String qrPngBase64,
        long expiresAtEpochSeconds
) {}
