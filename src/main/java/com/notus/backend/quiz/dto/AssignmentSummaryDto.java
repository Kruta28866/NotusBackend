package com.notus.backend.quiz.dto;

import java.time.Instant;

public record AssignmentSummaryDto(
        Long id,
        Long quizId,
        String quizTitle,
        String scheduleId,
        String scheduleSubject,
        String scheduleDate,
        String scheduleTime,
        Instant assignedAt,
        int submissionCount,
        double avgScore
) {}
