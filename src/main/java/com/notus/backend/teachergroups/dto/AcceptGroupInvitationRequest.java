package com.notus.backend.teachergroups.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptGroupInvitationRequest(@NotBlank String token) {}
