package com.notus.backend.realtime.dto;

import java.time.Instant;
import java.util.Map;

public record TeacherRealtimeEvent(
        String type,
        Instant occurredAt,
        Map<String, Object> payload
) {
    public static TeacherRealtimeEvent of(String type, Map<String, Object> payload) {
        return new TeacherRealtimeEvent(type, Instant.now(), payload);
    }
}
