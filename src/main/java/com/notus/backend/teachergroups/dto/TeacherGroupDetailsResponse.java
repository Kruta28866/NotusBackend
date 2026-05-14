package com.notus.backend.teachergroups.dto;

public record TeacherGroupDetailsResponse(
        Long id,
        String name,
        String description,
        String subject,
        String schoolYear,
        String semester,
        long studentsCount
) {}
