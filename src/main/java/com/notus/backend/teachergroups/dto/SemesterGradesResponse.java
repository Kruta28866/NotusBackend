package com.notus.backend.teachergroups.dto;

import java.util.List;

public record SemesterGradesResponse(
        String semester,
        double averageGrade,
        List<GradeTableRowResponse> grades
) {}
