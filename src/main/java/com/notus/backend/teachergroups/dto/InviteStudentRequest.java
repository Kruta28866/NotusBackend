package com.notus.backend.teachergroups.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteStudentRequest(@NotBlank @Email String email) {}
