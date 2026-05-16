package com.notus.backend.analytics;

import com.notus.backend.analytics.dto.*;
import com.notus.backend.attendance.AttendanceRecord;
import com.notus.backend.attendance.AttendanceRecordRepository;
import com.notus.backend.attendance.AttendanceSession;
import com.notus.backend.attendance.AttendanceSessionRepository;
import com.notus.backend.grades.Grade;
import com.notus.backend.grades.GradeAverageCalculator;
import com.notus.backend.grades.GradeRepository;
import com.notus.backend.quiz.*;
import com.notus.backend.teachergroups.*;
import com.notus.backend.users.Student;
import com.notus.backend.users.Teacher;
import com.notus.backend.users.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeacherAnalyticsService {

    private static final double LOW_ATTENDANCE_THRESHOLD = 70.0;
    private static final double LOW_GRADE_THRESHOLD = 3.0;

    private final UserService userService;
    private final TeacherGroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final GradeRepository gradeRepository;
    private final GradeAverageCalculator averageCalculator;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final QuizRepository quizRepository;
    private final QuizAssignmentRepository assignmentRepository;
    private final QuizSubmissionRepository submissionRepository;

    public TeacherAnalyticsService(UserService userService,
                                   TeacherGroupRepository groupRepository,
                                   GroupMembershipRepository membershipRepository,
                                   GradeRepository gradeRepository,
                                   GradeAverageCalculator averageCalculator,
                                   AttendanceSessionRepository sessionRepository,
                                   AttendanceRecordRepository recordRepository,
                                   QuizRepository quizRepository,
                                   QuizAssignmentRepository assignmentRepository,
                                   QuizSubmissionRepository submissionRepository) {
        this.userService = userService;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.gradeRepository = gradeRepository;
        this.averageCalculator = averageCalculator;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.quizRepository = quizRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public TeacherDashboardAnalyticsResponse dashboard(String teacherUid) {
        Teacher teacher = userService.findTeacherByUid(teacherUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tylko nauczyciel ma dostęp do analityki."));

        List<TeacherGroup> groups = groupRepository.findByTeacherAndActiveTrueOrderByCreatedAtDesc(teacher);
        List<GroupMembership> memberships = groups.stream()
                .flatMap(group -> membershipRepository.findByGroupAndStatusOrderByJoinedAtAsc(group, GroupMembershipStatus.ACTIVE).stream())
                .toList();
        List<Student> distinctStudents = memberships.stream()
                .map(GroupMembership::getStudent)
                .collect(Collectors.toMap(Student::getId, Function.identity(), (left, right) -> left))
                .values()
                .stream()
                .toList();

        List<AttendanceSession> sessions = sessionRepository.findByTeacher(teacher);
        Map<Long, List<AttendanceRecord>> recordsBySessionId = sessions.stream()
                .collect(Collectors.toMap(
                        AttendanceSession::getId,
                        session -> recordRepository.findBySessionId(session.getId()),
                        (left, right) -> left
                ));

        List<Grade> grades = gradeRepository.findByTeacherAndDeletedAtIsNull(teacher);
        List<Quiz> quizzes = quizRepository.findByTeacherAndArchivedFalse(teacher);
        List<QuizAssignment> assignments = assignmentRepository.findByTeacher(teacher);
        List<QuizSubmission> submissions = submissionRepository.findByAssignment_Teacher(teacher);

        Map<Long, List<Grade>> gradesByGroupId = grades.stream()
                .filter(grade -> grade.getGroup() != null)
                .collect(Collectors.groupingBy(grade -> grade.getGroup().getId()));
        Map<Long, List<Quiz>> quizzesByGroupId = quizzes.stream()
                .filter(quiz -> quiz.getGroup() != null)
                .collect(Collectors.groupingBy(quiz -> quiz.getGroup().getId()));
        Map<Long, List<QuizSubmission>> submissionsByGroupId = submissions.stream()
                .filter(submission -> submission.getAssignment() != null
                        && submission.getAssignment().getQuiz() != null
                        && submission.getAssignment().getQuiz().getGroup() != null)
                .collect(Collectors.groupingBy(submission -> submission.getAssignment().getQuiz().getGroup().getId()));

        List<StudentRiskResponse> risks = groups.stream()
                .flatMap(group -> membershipRepository.findByGroupAndStatusOrderByJoinedAtAsc(group, GroupMembershipStatus.ACTIVE).stream()
                        .map(membership -> riskForStudent(group, membership, sessionsForGroup(group, sessions), recordsBySessionId))
                        .flatMap(Optional::stream))
                .sorted(Comparator
                        .comparing(StudentRiskResponse::riskLevel)
                        .thenComparing(StudentRiskResponse::attendancePercentage)
                        .thenComparing(response -> response.averageGrade() == null ? 10.0 : response.averageGrade()))
                .limit(10)
                .toList();

        List<GroupAnalyticsResponse> groupResponses = groups.stream()
                .map(group -> {
                    List<GroupMembership> groupMemberships = membershipRepository.findByGroupAndStatusOrderByJoinedAtAsc(group, GroupMembershipStatus.ACTIVE);
                    List<AttendanceSession> groupSessions = sessionsForGroup(group, sessions);
                    List<Grade> groupGrades = gradesByGroupId.getOrDefault(group.getId(), List.of());
                    return new GroupAnalyticsResponse(
                            group.getId(),
                            group.getName(),
                            group.getSubject(),
                            group.getSchoolYear(),
                            group.getSemester(),
                            groupMemberships.size(),
                            attendancePercentage(groupMemberships, groupSessions, recordsBySessionId),
                            average(groupGrades),
                            groupGrades.size(),
                            quizzesByGroupId.getOrDefault(group.getId(), List.of()).size(),
                            submissionsByGroupId.getOrDefault(group.getId(), List.of()).size(),
                            risks.stream().filter(risk -> Objects.equals(risk.groupId(), group.getId())).count()
                    );
                })
                .toList();

        TeacherAnalyticsOverviewResponse overview = new TeacherAnalyticsOverviewResponse(
                groups.size(),
                distinctStudents.size(),
                sessions.size(),
                attendancePercentage(memberships, sessions, recordsBySessionId),
                grades.size(),
                average(grades),
                quizzes.size(),
                assignments.size(),
                submissions.size(),
                submissions.stream().filter(QuizSubmission::isPendingOpenReview).count()
        );

        return new TeacherDashboardAnalyticsResponse(
                Instant.now(),
                overview,
                groupResponses,
                risks,
                activityTrend(sessions, recordsBySessionId, grades, submissions)
        );
    }

    private Optional<StudentRiskResponse> riskForStudent(TeacherGroup group,
                                                        GroupMembership membership,
                                                        List<AttendanceSession> sessions,
                                                        Map<Long, List<AttendanceRecord>> recordsBySessionId) {
        Student student = membership.getStudent();
        double attendance = attendancePercentage(List.of(membership), sessions, recordsBySessionId);
        List<Grade> grades = gradeRepository.findByGroupAndStudentAndDeletedAtIsNullOrderByGradeDateDesc(group, student);
        Double average = average(grades);
        long missedSessions = sessions.stream()
                .filter(session -> recordsBySessionId.getOrDefault(session.getId(), List.of()).stream()
                        .noneMatch(record -> Objects.equals(record.getStudent().getId(), student.getId())))
                .count();

        List<String> reasons = new ArrayList<>();
        if (!sessions.isEmpty() && attendance < LOW_ATTENDANCE_THRESHOLD) {
            reasons.add("Niska frekwencja");
        }
        if (average != null && average < LOW_GRADE_THRESHOLD) {
            reasons.add("Niska średnia ocen");
        }

        if (reasons.isEmpty()) {
            return Optional.empty();
        }

        String riskLevel = attendance < 50.0 || (average != null && average < 2.0) ? "HIGH" : "MEDIUM";
        return Optional.of(new StudentRiskResponse(
                student.getId(),
                displayName(membership),
                email(membership),
                group.getId(),
                group.getName(),
                attendance,
                average,
                missedSessions,
                grades.size(),
                riskLevel,
                reasons
        ));
    }

    private List<ActivityTrendResponse> activityTrend(List<AttendanceSession> sessions,
                                                      Map<Long, List<AttendanceRecord>> recordsBySessionId,
                                                      List<Grade> grades,
                                                      List<QuizSubmission> submissions) {
        LocalDate currentWeek = weekStart(LocalDate.now());
        List<LocalDate> weeks = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            weeks.add(currentWeek.minusWeeks(i));
        }

        return weeks.stream()
                .map(weekStart -> {
                    LocalDate weekEnd = weekStart.plusDays(7);
                    long sessionsCount = sessions.stream()
                            .filter(session -> inWeek(toLocalDate(session.getCreatedAt()), weekStart, weekEnd))
                            .count();
                    long checkIns = sessions.stream()
                            .filter(session -> inWeek(toLocalDate(session.getCreatedAt()), weekStart, weekEnd))
                            .mapToLong(session -> recordsBySessionId.getOrDefault(session.getId(), List.of()).size())
                            .sum();
                    long gradesCount = grades.stream()
                            .filter(grade -> grade.getCreatedAt() != null)
                            .filter(grade -> inWeek(grade.getCreatedAt().toLocalDate(), weekStart, weekEnd))
                            .count();
                    long quizSubmissions = submissions.stream()
                            .filter(submission -> submission.getSubmittedAt() != null)
                            .filter(submission -> inWeek(toLocalDate(submission.getSubmittedAt()), weekStart, weekEnd))
                            .count();
                    return new ActivityTrendResponse(weekStart, sessionsCount, checkIns, gradesCount, quizSubmissions);
                })
                .toList();
    }

    private double attendancePercentage(List<GroupMembership> memberships,
                                        List<AttendanceSession> sessions,
                                        Map<Long, List<AttendanceRecord>> recordsBySessionId) {
        if (memberships.isEmpty() || sessions.isEmpty()) {
            return 0.0;
        }

        Set<Long> studentIds = memberships.stream()
                .map(membership -> membership.getStudent().getId())
                .collect(Collectors.toSet());
        long expected = (long) memberships.size() * sessions.size();
        long present = sessions.stream()
                .flatMap(session -> recordsBySessionId.getOrDefault(session.getId(), List.of()).stream())
                .filter(record -> studentIds.contains(record.getStudent().getId()))
                .count();

        return round2((present * 100.0) / expected);
    }

    private List<AttendanceSession> sessionsForGroup(TeacherGroup group, List<AttendanceSession> sessions) {
        if (group.getSubject() == null || group.getSubject().isBlank()) {
            return sessions;
        }
        String subject = group.getSubject().trim();
        List<AttendanceSession> matching = sessions.stream()
                .filter(session -> session.getSchedule() != null && session.getSchedule().getSubject() != null)
                .filter(session -> session.getSchedule().getSubject().equalsIgnoreCase(subject))
                .toList();
        return matching.isEmpty() ? sessions : matching;
    }

    private Double average(List<Grade> grades) {
        BigDecimal average = averageCalculator.weightedAverage(grades);
        return average == null ? null : average.doubleValue();
    }

    private String displayName(GroupMembership membership) {
        return hasText(membership.getDisplayNameOverride())
                ? membership.getDisplayNameOverride()
                : membership.getStudent().getName();
    }

    private String email(GroupMembership membership) {
        return hasText(membership.getEmailOverride())
                ? membership.getEmailOverride()
                : membership.getStudent().getEmail();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean inWeek(LocalDate value, LocalDate weekStart, LocalDate weekEnd) {
        return !value.isBefore(weekStart) && value.isBefore(weekEnd);
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    private LocalDate toLocalDate(Instant instant) {
        return LocalDate.ofInstant(instant.truncatedTo(ChronoUnit.SECONDS), ZoneId.systemDefault());
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
