package com.notus.backend.analytics;

import com.notus.backend.analytics.dto.TeacherDashboardAnalyticsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/teacher/analytics")
public class TeacherAnalyticsController {

    private final TeacherAnalyticsService analyticsService;

    public TeacherAnalyticsController(TeacherAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public TeacherDashboardAnalyticsResponse dashboard(Principal principal) {
        return analyticsService.dashboard(principal.getName());
    }
}
