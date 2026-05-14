package com.notus.backend.teachergroups.dto;

public record GroupStudentTableRowResponse(
        Long id,
        String fullName,
        String email,
        double attendancePercentage,
        double averageGrade
) {}
