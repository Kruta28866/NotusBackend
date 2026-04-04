package com.notus.backend.quiz.dto;

import com.notus.backend.quiz.QuestionType;
import java.util.List;

public record MyQuizReviewAnswerDto(
        Long questionId,
        String questionText,
        QuestionType type,
        List<String> options,
        String correctAnswer, // For closed questions
        String studentAnswer,
        Boolean isCorrect // Can be null if pending review
) {}
