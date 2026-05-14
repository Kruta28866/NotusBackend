package com.notus.backend.teachergroups.dto;

import java.time.LocalDate;

public record StudentAttendanceRowResponse(
        Long lessonId,
        LocalDate date,
        String topic,
        boolean present
) {}
