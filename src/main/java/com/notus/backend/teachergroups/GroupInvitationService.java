package com.notus.backend.teachergroups;

import com.notus.backend.auth.HashService;
import com.notus.backend.teachergroups.dto.InviteStudentRequest;
import com.notus.backend.teachergroups.dto.InviteStudentResponse;
import com.notus.backend.teachergroups.dto.GroupInvitationPreviewResponse;
import com.notus.backend.users.Teacher;
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

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String GENERIC_INVITE_ERROR = "Nie udało się zaprosić ucznia. Skontaktuj się z administratorem.";

    private final GroupInvitationRepository invitationRepository;
    private final TeacherGroupService groupService;
    private final HashService hashService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String frontendBaseUrl;

    public GroupInvitationService(GroupInvitationRepository invitationRepository,
                                  TeacherGroupService groupService,
                                  HashService hashService,
                                  EmailService emailService,
                                  @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.invitationRepository = invitationRepository;
        this.groupService = groupService;
        this.hashService = hashService;
        this.emailService = emailService;
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
            emailService.sendGroupInvitation(email, group.getName(), inviteLink);
            return new InviteStudentResponse(true, "Zaproszenie zostało wysłane.");
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().is4xxClientError() && ex.getStatusCode() != HttpStatus.FORBIDDEN) {
                return new InviteStudentResponse(false, GENERIC_INVITE_ERROR);
            }
            throw ex;
        } catch (RuntimeException ex) {
            return new InviteStudentResponse(false, GENERIC_INVITE_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public GroupInvitationPreviewResponse preview(String rawToken) {
        GroupInvitation invitation = requirePendingByRawToken(rawToken);
        return new GroupInvitationPreviewResponse(true, invitation.getGroup().getName(), "Zaproszenie jest aktywne.");
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
