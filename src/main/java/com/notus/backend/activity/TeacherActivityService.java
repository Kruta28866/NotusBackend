package com.notus.backend.activity;

import com.notus.backend.activity.dto.TeacherActivityItemResponse;
import com.notus.backend.activity.dto.TeacherActivityResponse;
import com.notus.backend.activity.dto.TeacherNotificationResponse;
import com.notus.backend.activity.dto.TeacherNotificationsResponse;
import com.notus.backend.attendance.AttendanceSessionRepository;
import com.notus.backend.grades.GradeRepository;
import com.notus.backend.quiz.QuizSubmissionRepository;
import com.notus.backend.teachergroups.*;
import com.notus.backend.users.Teacher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TeacherActivityService {

    private final TeacherGroupService groupService;
    private final GroupInvitationRepository invitationRepository;
    private final GroupMembershipRepository membershipRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    public TeacherActivityService(TeacherGroupService groupService,
                                  GroupInvitationRepository invitationRepository,
                                  GroupMembershipRepository membershipRepository,
                                  GradeRepository gradeRepository,
                                  AttendanceSessionRepository attendanceSessionRepository,
                                  QuizSubmissionRepository quizSubmissionRepository) {
        this.groupService = groupService;
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.gradeRepository = gradeRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public TeacherNotificationsResponse notifications(String teacherUid) {
        Teacher teacher = groupService.currentTeacher(teacherUid);
        Instant now = Instant.now();
        List<TeacherNotificationResponse> notifications = new ArrayList<>();

        invitationRepository.findByCreatedByTeacherOrderByCreatedAtDesc(teacher).stream()
                .limit(30)
                .forEach(invitation -> {
                    String actionUrl = "/teacher/groups/" + invitation.getGroup().getId();
                    if (invitation.getStatus() == GroupInvitationStatus.FAILED) {
                        notifications.add(notification(
                                "invitation-failed-" + invitation.getId(),
                                "INVITATION_FAILED",
                                "Zaproszenie nie zostało wysłane",
                                "Adres: " + invitation.getEmail() + " | Grupa: " + invitation.getGroup().getName(),
                                "error",
                                invitation.getCreatedAt(),
                                actionUrl
                        ));
                    } else if (invitation.getStatus() == GroupInvitationStatus.PENDING
                            && invitation.getExpiresAt().isBefore(now.plus(24, ChronoUnit.HOURS))) {
                        notifications.add(notification(
                                "invitation-expiring-" + invitation.getId(),
                                "INVITATION_EXPIRING",
                                "Zaproszenie niedługo wygaśnie",
                                invitation.getEmail() + " nie zaakceptował jeszcze zaproszenia do " + invitation.getGroup().getName() + ".",
                                "warning",
                                invitation.getExpiresAt(),
                                actionUrl
                        ));
                    } else if (invitation.getStatus() == GroupInvitationStatus.ACCEPTED
                            && invitation.getAcceptedAt() != null
                            && hasActiveMembership(invitation)) {
                        notifications.add(notification(
                                "invitation-accepted-" + invitation.getId(),
                                "INVITATION_ACCEPTED",
                                "Uczeń dołączył do grupy",
                                invitation.getEmail() + " zaakceptował zaproszenie do " + invitation.getGroup().getName() + ".",
                                "success",
                                invitation.getAcceptedAt(),
                                actionUrl
                        ));
                    }
                });

        notifications.sort(Comparator.comparing(TeacherNotificationResponse::createdAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        List<TeacherNotificationResponse> limited = notifications.stream().limit(12).toList();
        return new TeacherNotificationsResponse(limited, limited.stream().filter(item -> !item.read()).count(), now);
    }

    @Transactional(readOnly = true)
    public TeacherActivityResponse activity(String teacherUid) {
        Teacher teacher = groupService.currentTeacher(teacherUid);
        List<TeacherActivityItemResponse> items = new ArrayList<>();

        invitationRepository.findByCreatedByTeacherOrderByCreatedAtDesc(teacher).stream()
                .filter(invitation -> invitation.getStatus() != GroupInvitationStatus.ACCEPTED || hasActiveMembership(invitation))
                .limit(12)
                .forEach(invitation -> items.add(new TeacherActivityItemResponse(
                        "invitation-" + invitation.getId(),
                        "INVITATION",
                        invitationTitle(invitation),
                        invitation.getEmail() + " | " + invitation.getGroup().getName(),
                        invitation.getStatus() == GroupInvitationStatus.ACCEPTED ? "mark_email_read" : "outgoing_mail",
                        invitationTime(invitation),
                        "/teacher/groups/" + invitation.getGroup().getId()
                )));

        membershipRepository.findByTeacherOrderByJoinedAtDesc(teacher).stream()
                .filter(membership -> membership.getStatus() == GroupMembershipStatus.ACTIVE)
                .limit(12)
                .forEach(membership -> items.add(new TeacherActivityItemResponse(
                        "membership-" + membership.getId(),
                        "GROUP_MEMBER",
                        "Uczeń w grupie",
                        displayStudent(membership) + " | " + membership.getGroup().getName(),
                        "group_add",
                        membership.getJoinedAt(),
                        "/teacher/groups/" + membership.getGroup().getId()
                )));

        gradeRepository.findTop10ByTeacherAndDeletedAtIsNullOrderByCreatedAtDesc(teacher).forEach(grade ->
                items.add(new TeacherActivityItemResponse(
                        "grade-" + grade.getId(),
                        "GRADE",
                        "Wystawiono ocenę " + grade.getValue(),
                        (grade.getStudent() != null ? grade.getStudent().getName() : grade.getClerkUserId())
                                + " | " + (grade.getGroup() != null ? grade.getGroup().getName() : grade.getSubject()),
                        "grade",
                        toInstant(grade.getCreatedAt()),
                        grade.getGroup() != null && grade.getStudent() != null
                                ? "/teacher/groups/" + grade.getGroup().getId() + "/students/" + grade.getStudent().getId() + "/grades"
                                : "/teacher/stats"
                )));

        attendanceSessionRepository.findTop10ByTeacherOrderByCreatedAtDesc(teacher).forEach(session ->
                items.add(new TeacherActivityItemResponse(
                        "attendance-session-" + session.getId(),
                        "ATTENDANCE_SESSION",
                        session.isActive() ? "Uruchomiono sesję obecności" : "Sesja obecności",
                        session.getTitle() != null ? session.getTitle() : "Kod: " + session.getShortCode(),
                        "qr_code_2",
                        session.getCreatedAt(),
                        "/teacher/attendance/" + session.getId()
                )));

        quizSubmissionRepository.findTop10ByAssignment_TeacherOrderBySubmittedAtDesc(teacher).forEach(submission ->
                items.add(new TeacherActivityItemResponse(
                        "quiz-submission-" + submission.getId(),
                        "QUIZ_SUBMISSION",
                        "Uczeń rozwiązał quiz",
                        submission.getStudent().getName() + " | " + submission.getAssignment().getQuiz().getTitle(),
                        "quiz",
                        submission.getSubmittedAt(),
                        "/teacher/review/" + submission.getId()
                )));

        List<TeacherActivityItemResponse> sorted = items.stream()
                .filter(item -> item.occurredAt() != null)
                .sorted(Comparator.comparing(TeacherActivityItemResponse::occurredAt).reversed())
                .limit(25)
                .toList();

        return new TeacherActivityResponse(sorted, Instant.now());
    }

    private TeacherNotificationResponse notification(String id,
                                                     String type,
                                                     String title,
                                                     String body,
                                                     String severity,
                                                     Instant createdAt,
                                                     String actionUrl) {
        return new TeacherNotificationResponse(id, type, title, body, severity, false, createdAt, actionUrl);
    }

    private boolean hasActiveMembership(GroupInvitation invitation) {
        return invitation.getAcceptedBy() != null
                && membershipRepository.existsByGroupAndStudentAndStatus(
                invitation.getGroup(),
                invitation.getAcceptedBy(),
                GroupMembershipStatus.ACTIVE
        );
    }

    private String invitationTitle(GroupInvitation invitation) {
        return switch (invitation.getStatus()) {
            case ACCEPTED -> "Zaproszenie zaakceptowane";
            case FAILED -> "Błąd wysyłki zaproszenia";
            case CANCELLED -> "Zaproszenie anulowane";
            case EXPIRED -> "Zaproszenie wygasło";
            case PENDING -> "Wysłano zaproszenie";
        };
    }

    private Instant invitationTime(GroupInvitation invitation) {
        if (invitation.getAcceptedAt() != null) {
            return invitation.getAcceptedAt();
        }
        return invitation.getCreatedAt();
    }

    private String displayStudent(GroupMembership membership) {
        if (membership.getDisplayNameOverride() != null && !membership.getDisplayNameOverride().isBlank()) {
            return membership.getDisplayNameOverride();
        }
        return membership.getStudent().getName();
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
