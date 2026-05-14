package com.notus.backend.teachergroups;

import com.notus.backend.attendance.AttendanceRecordRepository;
import com.notus.backend.attendance.AttendanceSession;
import com.notus.backend.attendance.AttendanceSessionRepository;
import com.notus.backend.grades.Grade;
import com.notus.backend.grades.GradeRepository;
import com.notus.backend.teachergroups.dto.*;
import com.notus.backend.users.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TeacherStudentSummaryService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final GradeRepository gradeRepository;
    private final TeacherGroupService groupService;
    private final GroupMembershipService membershipService;

    public TeacherStudentSummaryService(AttendanceSessionRepository sessionRepository,
                                        AttendanceRecordRepository recordRepository,
                                        GradeRepository gradeRepository,
                                        TeacherGroupService groupService,
                                        GroupMembershipService membershipService) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.gradeRepository = gradeRepository;
        this.groupService = groupService;
        this.membershipService = membershipService;
    }

    @Transactional(readOnly = true)
    public List<GroupStudentTableRowResponse> listStudents(String teacherUid, Long groupId) {
        return membershipService.listStudents(teacherUid, groupId, this);
    }

    @Transactional(readOnly = true)
    public StudentAttendanceTableResponse attendance(String teacherUid, Long groupId, Long studentId) {
        TeacherGroup group = groupService.requireOwnedGroup(teacherUid, groupId);
        GroupMembership membership = membershipService.requireActiveMembership(group, studentId);
        Student student = membership.getStudent();
        List<AttendanceSession> sessions = sessionRepository.findByTeacher(group.getTeacher()).stream()
                .sorted(Comparator.comparing(AttendanceSession::getCreatedAt).reversed())
                .toList();

        List<StudentAttendanceRowResponse> items = sessions.stream()
                .map(session -> new StudentAttendanceRowResponse(
                        session.getId(),
                        toLocalDate(session),
                        resolveTopic(session),
                        recordRepository.findBySessionIdAndStudent(session.getId(), student).isPresent()
                ))
                .toList();

        return new StudentAttendanceTableResponse(
                student.getId(),
                displayName(membership),
                calculateAttendancePercentage(items),
                items
        );
    }

    @Transactional(readOnly = true)
    public StudentGradesBySemesterResponse grades(String teacherUid, Long groupId, Long studentId) {
        TeacherGroup group = groupService.requireOwnedGroup(teacherUid, groupId);
        GroupMembership membership = membershipService.requireActiveMembership(group, studentId);
        Student student = membership.getStudent();

        List<Grade> grades = gradeRepository.findByClerkUserIdOrderByIssueDateDesc(student.getClerkUserId()).stream()
                .filter(grade -> grade.getSubject() == null || group.getSubject() == null || grade.getSubject().equalsIgnoreCase(group.getSubject()))
                .toList();

        Map<String, List<Grade>> grouped = grades.stream()
                .collect(Collectors.groupingBy(grade -> semesterFor(grade.getIssueDate().getMonthValue())));

        List<SemesterGradesResponse> semesters = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SemesterGradesResponse(
                        entry.getKey(),
                        round2(average(entry.getValue())),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(Grade::getIssueDate).reversed())
                                .map(this::toGradeRow)
                                .toList()
                ))
                .toList();

        return new StudentGradesBySemesterResponse(
                student.getId(),
                displayName(membership),
                round2(average(grades)),
                semesters
        );
    }

    public GroupStudentTableRowResponse toStudentRow(TeacherGroup group, GroupMembership membership) {
        Student student = membership.getStudent();
        StudentAttendanceTableResponse attendance = attendanceForMembership(group, membership);
        double averageGrade = round2(average(
                gradeRepository.findByClerkUserIdOrderByIssueDateDesc(student.getClerkUserId()).stream()
                        .filter(grade -> grade.getSubject() == null || group.getSubject() == null || grade.getSubject().equalsIgnoreCase(group.getSubject()))
                        .toList()
        ));

        return new GroupStudentTableRowResponse(
                student.getId(),
                displayName(membership),
                email(membership),
                attendance.attendancePercentage(),
                averageGrade
        );
    }

    private StudentAttendanceTableResponse attendanceForMembership(TeacherGroup group, GroupMembership membership) {
        Student student = membership.getStudent();
        List<StudentAttendanceRowResponse> items = sessionRepository.findByTeacher(group.getTeacher()).stream()
                .map(session -> new StudentAttendanceRowResponse(
                        session.getId(),
                        toLocalDate(session),
                        resolveTopic(session),
                        recordRepository.findBySessionIdAndStudent(session.getId(), student).isPresent()
                ))
                .toList();
        return new StudentAttendanceTableResponse(student.getId(), displayName(membership), calculateAttendancePercentage(items), items);
    }

    private double calculateAttendancePercentage(List<StudentAttendanceRowResponse> items) {
        if (items.isEmpty()) {
            return 0.0;
        }
        long present = items.stream().filter(StudentAttendanceRowResponse::present).count();
        return round2((present * 100.0) / items.size());
    }

    private double average(List<Grade> grades) {
        List<Double> values = grades.stream()
                .map(grade -> numericValue(grade.getValue()))
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private GradeTableRowResponse toGradeRow(Grade grade) {
        double numeric = Objects.requireNonNullElse(numericValue(grade.getValue()), 0.0);
        return new GradeTableRowResponse(
                grade.getId(),
                grade.getIssueDate().toLocalDate(),
                grade.getValue(),
                numeric,
                grade.getSubject(),
                ""
        );
    }

    private Double numericValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim()) {
            case "6" -> 6.0;
            case "5+" -> 5.5;
            case "5" -> 5.0;
            case "5-" -> 4.75;
            case "4+" -> 4.5;
            case "4" -> 4.0;
            case "4-" -> 3.75;
            case "3+" -> 3.5;
            case "3" -> 3.0;
            case "3-" -> 2.75;
            case "2+" -> 2.5;
            case "2" -> 2.0;
            case "2-" -> 1.75;
            case "1" -> 1.0;
            default -> null;
        };
    }

    private String semesterFor(int month) {
        return month >= 2 && month <= 8 ? "2" : "1";
    }

    private LocalDate toLocalDate(AttendanceSession session) {
        if (session.getSchedule() != null && session.getSchedule().getDate() != null) {
            return LocalDate.ofInstant(session.getSchedule().getDate(), ZoneId.systemDefault());
        }
        return LocalDate.ofInstant(session.getCreatedAt(), ZoneId.systemDefault());
    }

    private String resolveTopic(AttendanceSession session) {
        if (session.getSchedule() != null && session.getSchedule().getSubject() != null) {
            return session.getSchedule().getSubject();
        }
        if (session.getTitle() != null && !session.getTitle().isBlank()) {
            return session.getTitle();
        }
        return "Zajęcia";
    }

    private String displayName(GroupMembership membership) {
        return hasText(membership.getDisplayNameOverride()) ? membership.getDisplayNameOverride() : membership.getStudent().getName();
    }

    private String email(GroupMembership membership) {
        return hasText(membership.getEmailOverride()) ? membership.getEmailOverride() : membership.getStudent().getEmail();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
