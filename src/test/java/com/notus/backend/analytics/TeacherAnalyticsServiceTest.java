package com.notus.backend.analytics;

import com.notus.backend.analytics.dto.TeacherDashboardAnalyticsResponse;
import com.notus.backend.attendance.AttendanceRecord;
import com.notus.backend.attendance.AttendanceRecordRepository;
import com.notus.backend.attendance.AttendanceSession;
import com.notus.backend.attendance.AttendanceSessionRepository;
import com.notus.backend.grades.Grade;
import com.notus.backend.grades.GradeAverageCalculator;
import com.notus.backend.grades.GradeRepository;
import com.notus.backend.quiz.*;
import com.notus.backend.teachergroups.*;
import com.notus.backend.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherAnalyticsServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private TeacherGroupRepository groupRepository;
    @Mock
    private GroupMembershipRepository membershipRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAssignmentRepository assignmentRepository;
    @Mock
    private QuizSubmissionRepository submissionRepository;

    private TeacherAnalyticsService service;
    private Teacher teacher;
    private TeacherGroup group;
    private Student student;
    private GroupMembership membership;

    @BeforeEach
    void setUp() {
        service = new TeacherAnalyticsService(
                userService,
                groupRepository,
                membershipRepository,
                gradeRepository,
                new GradeAverageCalculator(),
                sessionRepository,
                recordRepository,
                quizRepository,
                assignmentRepository,
                submissionRepository
        );

        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setClerkUserId("teacher-uid");
        teacher.setEmail("teacher@example.com");
        teacher.setName("Teacher");
        teacher.setRole(Role.TEACHER);

        group = new TeacherGroup();
        group.setId(10L);
        group.setTeacher(teacher);
        group.setName("Matematyka 1A");
        group.setSubject("Matematyka");
        group.setSchoolYear("2025/2026");
        group.setSemester("2");
        group.setActive(true);
        group.setCreatedAt(Instant.now());

        student = new Student();
        student.setId(20L);
        student.setClerkUserId("student-uid");
        student.setEmail("student@example.com");
        student.setName("Anna Kowalska");
        student.setRole(Role.STUDENT);

        membership = new GroupMembership();
        membership.setGroup(group);
        membership.setStudent(student);
        membership.setStatus(GroupMembershipStatus.ACTIVE);
    }

    @Test
    void dashboardCountsTeacherAnalyticsFromExistingData() {
        AttendanceSession firstSession = session(100L);
        AttendanceSession secondSession = session(101L);
        AttendanceRecord presentRecord = new AttendanceRecord();
        presentRecord.setSessionId(100L);
        presentRecord.setStudent(student);
        presentRecord.setCheckedInAt(Instant.now());

        Grade weakGrade = grade(2L, "2", BigDecimal.valueOf(2.0), 1);
        Grade betterGrade = grade(3L, "4", BigDecimal.valueOf(4.0), 1);

        Quiz quiz = new Quiz();
        quiz.setId(30L);
        quiz.setTeacher(teacher);
        quiz.setGroup(group);
        quiz.setTitle("Quiz 1");

        QuizAssignment assignment = new QuizAssignment();
        assignment.setId(40L);
        assignment.setTeacher(teacher);
        assignment.setQuiz(quiz);
        assignment.setScheduleId("schedule-1");

        QuizSubmission submission = new QuizSubmission();
        submission.setId(50L);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setScore(4);
        submission.setTotal(5);
        submission.setPendingOpenReview(true);
        submission.setSubmittedAt(Instant.now());

        when(userService.findTeacherByUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(groupRepository.findByTeacherAndActiveTrueOrderByCreatedAtDesc(teacher)).thenReturn(List.of(group));
        when(membershipRepository.findByGroupAndStatusOrderByJoinedAtAsc(group, GroupMembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));
        when(sessionRepository.findByTeacher(teacher)).thenReturn(List.of(firstSession, secondSession));
        when(recordRepository.findBySessionId(100L)).thenReturn(List.of(presentRecord));
        when(recordRepository.findBySessionId(101L)).thenReturn(List.of());
        when(gradeRepository.findByTeacherAndDeletedAtIsNull(teacher)).thenReturn(List.of(weakGrade, betterGrade));
        when(gradeRepository.findByGroupAndStudentAndDeletedAtIsNullOrderByGradeDateDesc(group, student))
                .thenReturn(List.of(weakGrade, betterGrade));
        when(quizRepository.findByTeacherAndArchivedFalse(teacher)).thenReturn(List.of(quiz));
        when(assignmentRepository.findByTeacher(teacher)).thenReturn(List.of(assignment));
        when(submissionRepository.findByAssignment_Teacher(teacher)).thenReturn(List.of(submission));

        TeacherDashboardAnalyticsResponse result = service.dashboard("teacher-uid");

        assertEquals(1, result.overview().activeGroups());
        assertEquals(1, result.overview().activeStudents());
        assertEquals(2, result.overview().attendanceSessions());
        assertEquals(50.0, result.overview().attendancePercentage());
        assertEquals(2, result.overview().gradesCount());
        assertEquals(3.0, result.overview().averageGrade());
        assertEquals(1, result.overview().quizzesCount());
        assertEquals(1, result.overview().quizAssignmentsCount());
        assertEquals(1, result.overview().quizSubmissionsCount());
        assertEquals(1, result.overview().pendingQuizReviews());

        assertEquals(1, result.groups().size());
        assertEquals(50.0, result.groups().getFirst().attendancePercentage());
        assertEquals(3.0, result.groups().getFirst().averageGrade());
        assertEquals(1, result.groups().getFirst().studentsAtRisk());

        assertEquals(1, result.studentsAtRisk().size());
        assertEquals("Niska frekwencja", result.studentsAtRisk().getFirst().reasons().getFirst());
    }

    @Test
    void dashboardRejectsNonTeacherUid() {
        when(userService.findTeacherByUid("student-uid")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.dashboard("student-uid"));
    }

    private AttendanceSession session(Long id) {
        AttendanceSession session = new AttendanceSession();
        session.setId(id);
        session.setTeacher(teacher);
        session.setTitle("Zajęcia");
        session.setCreatedAt(Instant.now());
        session.setActive(false);
        return session;
    }

    private Grade grade(Long id, String value, BigDecimal numericValue, int weight) {
        Grade grade = new Grade();
        grade.setId(id);
        grade.setTeacher(teacher);
        grade.setStudent(student);
        grade.setGroup(group);
        grade.setClerkUserId(student.getClerkUserId());
        grade.setSubject("Matematyka");
        grade.setValue(value);
        grade.setNumericValue(numericValue);
        grade.setWeight(weight);
        grade.setSemester("2");
        grade.setSourceType("MANUAL");
        grade.setIssueDate(LocalDateTime.now());
        grade.setCreatedAt(LocalDateTime.now());
        return grade;
    }
}
