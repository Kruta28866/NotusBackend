package com.notus.backend.quiz.dto;

import java.util.List;

public record AssignmentResultsDto(
        Long assignmentId,
        String quizTitle,
        String scheduleSubject,
        String scheduleDate,
        String scheduleTime,
        List<StudentScoreDto> submissions
) {}
