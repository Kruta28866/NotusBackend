package com.notus.backend.schedule;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping("/today")
    public List<Schedule> getTodaySchedule(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String teacherName
    ) {
        return scheduleService.getTodayScheduleForTeacher(teacherId, teacherName);
    }

    @GetMapping("/by-day")
    public List<Schedule> getScheduleByDay(@RequestParam String date) {
        return scheduleService.getScheduleByDay(LocalDate.parse(date));
    }

    @GetMapping
    public List<Schedule> getSchedule(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String teacherName
    ) {
        return scheduleService.getSchedule(Instant.parse(start), Instant.parse(end), teacherId, teacherName);
    }
}
