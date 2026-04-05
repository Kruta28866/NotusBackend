package com.notus.backend.quiz.dto;

import com.notus.backend.quiz.QuizQuestion;

import java.time.Instant;
import java.util.List;

public record QuizDetailsDto(
        Long id,
        String title,
        String description,
        Instant createdAt,
        int version,
        List<QuizQuestion> questions,
        boolean hasSubmissions
) {}
