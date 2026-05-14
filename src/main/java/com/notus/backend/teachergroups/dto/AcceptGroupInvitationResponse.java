package com.notus.backend.teachergroups.dto;

public record AcceptGroupInvitationResponse(boolean success, String message, Long groupId, String groupName) {}
