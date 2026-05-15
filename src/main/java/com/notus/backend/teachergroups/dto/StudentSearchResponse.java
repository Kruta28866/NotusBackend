package com.notus.backend.teachergroups.dto;

public record StudentSearchResponse(
        Long id,
        String fullName,
        String email,
        boolean alreadyInGroup
) {
}
