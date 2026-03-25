package com.notus.backend.quiz.dto;

import com.notus.backend.quiz.QuestionType;

public record ReviewAnswerDto(
        Long answerId,
        Long questionId,
        String questionText,
        QuestionType questionType,
        String answerText,
        Boolean correct
) {}
