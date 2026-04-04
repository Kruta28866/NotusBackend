package com.notus.backend.schedule;

import com.notus.backend.users.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public List<Schedule> getTodaySchedule(Long teacherId, String teacherName, Long groupId) {
        LocalDate today = LocalDate.now();
        Instant start = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return getFilteredSchedule(start, end, teacherId, teacherName, groupId);
    }

    public List<Schedule> getScheduleByDay(LocalDate date) {
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return scheduleRepository.findByDateBetweenOrderByTimeAsc(start, end);
    }

    public List<Schedule> getSchedule(Instant start, Instant end, Long teacherId, String teacherName, Long groupId) {
        return getFilteredSchedule(start, end, teacherId, teacherName, groupId);
    }

    public List<Schedule> getScheduleForStudentInRange(Student student, Instant start, Instant end) {
        if (student.getStudentGroups() == null || student.getStudentGroups().isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = student.getStudentGroups()
                .stream()
                .map(group -> group.getId())
                .distinct()
                .toList();
        return scheduleRepository.findByDateBetweenAndStudentGroupIdInOrderByTimeAsc(start, end, groupIds);
    }

    public List<Schedule> getScheduleForStudent(Student student) {
        if (student.getStudentGroups() == null || student.getStudentGroups().isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = student.getStudentGroups()
                .stream()
                .map(group -> group.getId())
                .distinct()
                .toList();

        return scheduleRepository.findByStudentGroupIdInOrderByDateAscTimeAsc(groupIds);
    }

    private List<Schedule> getFilteredSchedule(
            Instant start,
            Instant end,
            Long teacherId,
            String teacherName,
            Long groupId
    ) {
        if (teacherId != null) {
            List<Schedule> byTeacherId = scheduleRepository.findByDateBetweenAndTeacherEntityIdOrderByTimeAsc(start, end, teacherId);
            if (!byTeacherId.isEmpty()) {
                return byTeacherId;
            }
        }

        if (teacherName != null && !teacherName.isBlank()) {
            List<Schedule> byTeacherName =
                    scheduleRepository.findByDateBetweenAndTeacherEntityNameContainingIgnoreCaseOrderByTimeAsc(start, end, teacherName);
            if (!byTeacherName.isEmpty()) {
                return byTeacherName;
            }
        }

        if (groupId != null) {
            return scheduleRepository.findByDateBetweenAndStudentGroupIdOrderByTimeAsc(start, end, groupId);
        }

        return scheduleRepository.findByDateBetweenOrderByTimeAsc(start, end);
    }

    public List<Schedule> getTodayScheduleForStudent(Student student) {
        LocalDate today = LocalDate.now();
        Instant start = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        if (student.getStudentGroups() == null || student.getStudentGroups().isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = student.getStudentGroups()
                .stream()
                .map(group -> group.getId())
                .distinct()
                .toList();

        return scheduleRepository.findByDateBetweenAndStudentGroupIdInOrderByTimeAsc(start, end, groupIds);
    }


}