package com.notus.backend.analytics.dto;

import java.util.List;

public record StudentRiskResponse(
        Long studentId,
        String studentName,
        String email,
        Long groupId,
        String groupName,
        double attendancePercentage,
        Double averageGrade,
        long missedSessions,
        long gradesCount,
        String riskLevel,
        List<String> reasons
) {
}
