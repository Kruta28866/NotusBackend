package com.notus.backend.quiz.dto;

public record NewReviewNotificationDto(
        Long submissionId,
        Long assignmentId,
        String quizTitle,
        int score,
        int total
) {}
