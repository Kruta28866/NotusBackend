package com.notus.backend.analytics.dto;

import java.time.Instant;
import java.util.List;

public record TeacherDashboardAnalyticsResponse(
        Instant generatedAt,
        TeacherAnalyticsOverviewResponse overview,
        List<GroupAnalyticsResponse> groups,
        List<StudentRiskResponse> studentsAtRisk,
        List<ActivityTrendResponse> activityTrend
) {
}
