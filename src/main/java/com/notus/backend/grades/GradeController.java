package com.notus.backend.grades;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping("/recent")
    public List<GradeDto> getRecentGrades(Authentication authentication) {
        String clerkUserId = (String) authentication.getPrincipal();
        return gradeService.getRecentGrades(clerkUserId);
    }
}
