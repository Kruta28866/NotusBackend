package com.notus.backend.quiz.dto;

import java.util.Map;

public record SubmitAnswersRequest(Map<Long, String> answers) {}
