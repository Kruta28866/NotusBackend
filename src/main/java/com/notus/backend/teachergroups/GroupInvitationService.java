package com.notus.backend.teachergroups;

import com.notus.backend.auth.HashService;
import com.notus.backend.teachergroups.dto.InviteStudentRequest;
import com.notus.backend.teachergroups.dto.InviteStudentResponse;
import com.notus.backend.teachergroups.dto.GroupInvitationPreviewResponse;
import com.notus.backend.users.Role;
import com.notus.backend.users.Student;
import com.notus.backend.users.StudentRepository;
import com.notus.backend.users.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
public class GroupInvitationService {

    private static final Logger log = LoggerFactory.getLogger(GroupInvitationService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String GENERIC_INVITE_ERROR = "Nie udało się zaprosić ucznia. Skontaktuj się z administratorem.";

    private final GroupInvitationRepository invitationRepository;
    private final TeacherGroupService groupService;
    private final HashService hashService;
    private final EmailService emailService;
    private final StudentRepository studentRepository;
    private final GroupMembershipRepository membershipRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String frontendBaseUrl;

    public GroupInvitationService(GroupInvitationRepository invitationRepository,
                                  TeacherGroupService groupService,
                                  HashService hashService,
                                  EmailService emailService,
                                  StudentRepository studentRepository,
                                  GroupMembershipRepository membershipRepository,
                                  @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.invitationRepository = invitationRepository;
        this.groupService = groupService;
        this.hashService = hashService;
        this.emailService = emailService;
        this.studentRepository = studentRepository;
        this.membershipRepository = membershipRepository;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public InviteStudentResponse invite(String teacherUid, Long groupId, InviteStudentRequest request) {
        try {
            String email = normalizeEmail(request.email());
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GENERIC_INVITE_ERROR);
            }

            TeacherGroup group = groupService.requireOwnedGroup(teacherUid, groupId);
            InviteStudentResponse existingStudentCheck = checkExistingStudent(email, group);
            if (existingStudentCheck != null) {
                return existingStudentCheck;
            }

            Teacher teacher = group.getTeacher();
            String rawToken = generateRawToken();

            GroupInvitation invitation = new GroupInvitation();
            invitation.setGroup(group);
            invitation.setEmail(email);
            invitation.setTokenHash(hashService.sha256(rawToken));
            invitation.setStatus(GroupInvitationStatus.PENDING);
            invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
            invitation.setCreatedByTeacher(teacher);
            invitationRepository.save(invitation);

            String inviteLink = frontendBaseUrl + "/invite/group?token=" + rawToken;
            try {
                emailService.sendGroupInvitation(email, group.getName(), teacher.getName(), inviteLink);
            } catch (RuntimeException ex) {
                invitation.setStatus(GroupInvitationStatus.FAILED);
                invitationRepository.save(invitation);
                throw ex;
            }
            return new InviteStudentResponse(true, "Zaproszenie zostało wysłane.");
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().is4xxClientError() && ex.getStatusCode() != HttpStatus.FORBIDDEN) {
                log.warn("Group invitation rejected for group {}: {}", groupId, ex.getReason());
                return new InviteStudentResponse(false, GENERIC_INVITE_ERROR);
            }
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Could not send group invitation for group {}", groupId, ex);
            return new InviteStudentResponse(false, GENERIC_INVITE_ERROR);
        }
    }

    private InviteStudentResponse checkExistingStudent(String email, TeacherGroup group) {
        Student student = studentRepository.findByEmailIgnoreCase(email).orElse(null);
        if (student == null) {
            return null;
        }

        if (student.getRole() != Role.STUDENT) {
            return new InviteStudentResponse(false, GENERIC_INVITE_ERROR);
        }

        if (membershipRepository.existsByGroupAndStudentAndStatus(group, student, GroupMembershipStatus.ACTIVE)) {
            return new InviteStudentResponse(false, "Ten uczeń jest już w grupie.");
        }

        return null;
    }

    @Transactional(readOnly = true)
    public GroupInvitationPreviewResponse preview(String rawToken) {
        try {
            GroupInvitation invitation = requirePendingByRawToken(rawToken);
            return new GroupInvitationPreviewResponse(
                    true,
                    invitation.getGroup().getId(),
                    invitation.getGroup().getName(),
                    invitation.getCreatedByTeacher().getName(),
                    invitation.getEmail(),
                    invitation.getExpiresAt(),
                    "Zaproszenie jest aktywne."
            );
        } catch (ResponseStatusException ex) {
            return new GroupInvitationPreviewResponse(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    ex.getReason() != null ? ex.getReason() : "Zaproszenie jest nieprawidłowe albo wygasło."
            );
        }
    }

    @Transactional(readOnly = true)
    public GroupInvitation requirePendingByRawToken(String rawToken) {
        GroupInvitation invitation = invitationRepository.findByTokenHash(hashService.sha256(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "To zaproszenie jest nieprawidłowe albo wygasło."));

        if (invitation.getStatus() == GroupInvitationStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "To zaproszenie zostało już wykorzystane.");
        }

        if (invitation.getStatus() != GroupInvitationStatus.PENDING || invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To zaproszenie jest nieprawidłowe albo wygasło.");
        }

        return invitation;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
