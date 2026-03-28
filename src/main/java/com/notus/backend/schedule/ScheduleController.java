package com.notus.backend.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/today")
    public List<Schedule> getTodaySchedule(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) Long groupId
    ) {
        return scheduleService.getTodaySchedule(teacherId, teacherName, groupId);
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
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) Long groupId
    ) {
        return scheduleService.getSchedule(
                Instant.parse(start),
                Instant.parse(end),
                teacherId,
                teacherName,
                groupId
        );
    }
}