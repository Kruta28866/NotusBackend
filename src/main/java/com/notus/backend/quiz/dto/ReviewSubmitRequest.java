package com.notus.backend.quiz.dto;

import java.util.Map;

/** answerId → correct (true/false) */
public record ReviewSubmitRequest(Map<Long, Boolean> marks) {}
