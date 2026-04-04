package com.notus.backend.quiz.dto;

import java.util.List;

public record MyQuizReviewDto(
        Long assignmentId,
        String quizTitle,
        String scheduleSubject,
        int score,
        int total,
        boolean pendingOpenReview,
        List<MyQuizReviewAnswerDto> answers
) {}
