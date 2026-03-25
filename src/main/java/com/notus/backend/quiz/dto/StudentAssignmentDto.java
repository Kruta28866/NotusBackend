package com.notus.backend.quiz.dto;

import java.util.List;

public record StudentAssignmentDto(
        Long assignmentId,
        String quizTitle,
        String scheduleSubject,
        String scheduleDate,
        String scheduleTime,
        List<StudentQuestionDto> questions,
        boolean alreadySubmitted,
        Integer myScore,
        Integer myTotal,
        boolean pendingOpenReview
) {}
