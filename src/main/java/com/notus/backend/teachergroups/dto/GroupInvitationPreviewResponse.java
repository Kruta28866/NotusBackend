package com.notus.backend.teachergroups.dto;

import java.time.Instant;

public record GroupInvitationPreviewResponse(
        boolean valid,
        Long groupId,
        String groupName,
        String teacherName,
        String email,
        Instant expiresAt,
        String message
) {}
