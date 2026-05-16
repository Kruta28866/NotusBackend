package com.notus.backend.analytics.dto;

public record GroupAnalyticsResponse(
        Long groupId,
        String groupName,
        String subject,
        String schoolYear,
        String semester,
        long studentsCount,
        double attendancePercentage,
        Double averageGrade,
        long gradesCount,
        long quizzesCount,
        long quizSubmissionsCount,
        long studentsAtRisk
) {
}
