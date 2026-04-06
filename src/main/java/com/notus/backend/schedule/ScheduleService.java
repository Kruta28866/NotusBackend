package com.notus.backend.schedule;

import com.notus.backend.attendance.group.StudentGroup;
import com.notus.backend.attendance.group.StudentGroupRepository;
import com.notus.backend.users.Student;
import com.notus.backend.users.Teacher;
import com.notus.backend.users.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TeacherRepository teacherRepository;
    private final StudentGroupRepository studentGroupRepository;

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


    @Transactional(readOnly = true)
    public Schedule getById(String id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
    }

    @Transactional
    public Schedule createSchedule(CreateScheduleRequest req, String clerkUid) {
        Teacher teacher = teacherRepository.findByClerkUserId(clerkUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher not found"));

        StudentGroup group = null;
        if (req.studentGroupId() != null) {
            group = studentGroupRepository.findById(req.studentGroupId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student group not found"));
        }

        Schedule schedule = Schedule.builder()
                .id(UUID.randomUUID().toString())
                .subject(req.subject())
                .date(req.date())
                .time(req.time())
                .room(req.room())
                .type(req.type())
                .color(req.color() != null ? req.color() : "primary")
                .teacherEntity(teacher)
                .studentGroup(group)
                .build();

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public Schedule updateSchedule(String id, CreateScheduleRequest req) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        // Full-replacement PUT semantics: null studentGroupId clears any existing group
        StudentGroup group = null;
        if (req.studentGroupId() != null) {
            group = studentGroupRepository.findById(req.studentGroupId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student group not found"));
        }

        schedule.setSubject(req.subject());
        schedule.setDate(req.date());
        schedule.setTime(req.time());
        schedule.setRoom(req.room());
        schedule.setType(req.type());
        schedule.setColor(req.color() != null ? req.color() : "primary");
        schedule.setStudentGroup(group);
        // teacherEntity is intentionally immutable after creation — not updated here
        // TODO: enforce ownership — only the creating teacher should be allowed to modify

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(String id) {
        if (!scheduleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        // TODO: enforce ownership — only the creating teacher should be allowed to delete
        scheduleRepository.deleteById(id);
    }
}
