package com.notus.backend.analytics.dto;

import java.time.LocalDate;

public record ActivityTrendResponse(
        LocalDate weekStart,
        long attendanceSessions,
        long attendanceCheckIns,
        long gradesCreated,
        long quizSubmissions
) {
}
