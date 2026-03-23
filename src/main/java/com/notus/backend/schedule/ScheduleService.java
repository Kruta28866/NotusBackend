package com.notus.backend.schedule;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;

    public List<Schedule> getTodayScheduleForTeacher(String teacherName) {
        LocalDate today = LocalDate.now();
        Instant start = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return scheduleRepository.findByDateBetweenAndTeacherContainingIgnoreCase(start, end, teacherName);
    }

    public List<Schedule> getScheduleByDay(LocalDate date) {
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return scheduleRepository.findByDateBetween(start, end);
    }

    public List<Schedule> getSchedule(Instant start, Instant end, String teacherName) {
        if (teacherName != null && !teacherName.isBlank()) {
            return scheduleRepository.findByDateBetweenAndTeacherContainingIgnoreCase(start, end, teacherName);
        }
        return scheduleRepository.findByDateBetween(start, end);
    }
}
