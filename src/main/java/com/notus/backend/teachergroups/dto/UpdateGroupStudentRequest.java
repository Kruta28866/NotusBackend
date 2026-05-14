package com.notus.backend.teachergroups.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateGroupStudentRequest(
        @NotBlank String displayName,
        @NotBlank @Email String email
) {}
