package com.notus.backend.analytics.dto;

public record TeacherAnalyticsOverviewResponse(
        long activeGroups,
        long activeStudents,
        long attendanceSessions,
        double attendancePercentage,
        long gradesCount,
        Double averageGrade,
        long quizzesCount,
        long quizAssignmentsCount,
        long quizSubmissionsCount,
        long pendingQuizReviews
) {
}
