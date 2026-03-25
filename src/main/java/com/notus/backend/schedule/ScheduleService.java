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

    public List<Schedule> getTodayScheduleForTeacher(Long teacherId, String teacherName) {
        LocalDate today = LocalDate.now();
        Instant start = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        if (teacherId != null) {
            List<Schedule> byId = scheduleRepository.findByDateBetweenAndTeacherEntityId(start, end, teacherId);
            if (!byId.isEmpty()) return byId;
        }
        if (teacherName != null && !teacherName.isBlank()) {
            return scheduleRepository.findByDateBetweenAndTeacherEntityNameContainingIgnoreCase(start, end, teacherName);
        }
        return List.of();
    }

    public List<Schedule> getScheduleByDay(LocalDate date) {
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return scheduleRepository.findByDateBetween(start, end);
    }

    public List<Schedule> getSchedule(Instant start, Instant end, Long teacherId, String teacherName) {
        if (teacherId != null) {
            List<Schedule> byId = scheduleRepository.findByDateBetweenAndTeacherEntityId(start, end, teacherId);
            if (!byId.isEmpty()) return byId;
        }
        if (teacherName != null && !teacherName.isBlank()) {
            return scheduleRepository.findByDateBetweenAndTeacherEntityNameContainingIgnoreCase(start, end, teacherName);
        }
        return scheduleRepository.findByDateBetween(start, end);
    }
}
