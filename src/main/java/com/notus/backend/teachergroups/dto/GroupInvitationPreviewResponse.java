package com.notus.backend.teachergroups.dto;

public record GroupInvitationPreviewResponse(
        boolean valid,
        String groupName,
        String message
) {}
