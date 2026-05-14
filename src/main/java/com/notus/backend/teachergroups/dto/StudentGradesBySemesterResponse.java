package com.notus.backend.teachergroups.dto;

import java.util.List;

public record StudentGradesBySemesterResponse(
        Long studentId,
        String studentName,
        double averageGrade,
        List<SemesterGradesResponse> semesters
) {}
