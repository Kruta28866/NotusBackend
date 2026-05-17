package com.notus.backend.activity.dto;

import java.time.Instant;
import java.util.List;

public record TeacherActivityResponse(
        List<TeacherActivityItemResponse> items,
        Instant generatedAt
) {}
