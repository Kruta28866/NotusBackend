package com.notus.backend.quiz.dto;

import java.time.Instant;

public record StudentScoreDto(
        String studentName,
        String indexNumber,
        int score,
        int total,
        Instant submittedAt,
        boolean pendingOpenReview,
        Long submissionId
) {}
