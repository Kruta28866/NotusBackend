package com.notus.backend.teachergroups.dto;

import java.time.LocalDate;

public record GradeTableRowResponse(
        Long id,
        LocalDate date,
        String value,
        double numericValue,
        String source,
        String comment
) {}
