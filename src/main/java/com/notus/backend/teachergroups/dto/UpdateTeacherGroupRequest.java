package com.notus.backend.teachergroups.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTeacherGroupRequest(
        @NotBlank String name,
        String description,
        String subject,
        String schoolYear,
        String semester
) {}
