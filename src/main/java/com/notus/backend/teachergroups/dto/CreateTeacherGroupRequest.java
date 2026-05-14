package com.notus.backend.teachergroups.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeacherGroupRequest(
        @NotBlank String name,
        String description,
        String subject,
        String schoolYear,
        String semester
) {}
