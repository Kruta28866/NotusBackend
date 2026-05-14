package com.notus.backend.teachergroups.dto;

import java.util.List;

public record StudentAttendanceTableResponse(
        Long studentId,
        String studentName,
        double attendancePercentage,
        List<StudentAttendanceRowResponse> items
) {}
