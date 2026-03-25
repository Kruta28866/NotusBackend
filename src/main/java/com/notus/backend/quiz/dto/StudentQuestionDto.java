package com.notus.backend.quiz.dto;

import com.notus.backend.quiz.QuestionType;

import java.util.List;

public record StudentQuestionDto(
        Long id,
        String questionText,
        QuestionType type,
        List<String> options
) {}
