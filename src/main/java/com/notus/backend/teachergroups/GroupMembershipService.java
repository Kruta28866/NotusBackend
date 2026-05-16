package com.notus.backend.teachergroups;

import com.notus.backend.realtime.TeacherRealtimeService;
import com.notus.backend.realtime.dto.TeacherRealtimeEvent;
import com.notus.backend.teachergroups.dto.*;
import com.notus.backend.users.Role;
import com.notus.backend.users.Student;
import com.notus.backend.users.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GroupMembershipService {

    private final GroupMembershipRepository membershipRepository;
    private final GroupInvitationRepository invitationRepository;
    private final GroupInvitationService invitationService;
    private final TeacherGroupService groupService;
    private final UserService userService;
    private final TeacherRealtimeService realtimeService;

    public GroupMembershipService(GroupMembershipRepository membershipRepository,
                                  GroupInvitationRepository invitationRepository,
                                  GroupInvitationService invitationService,
                                  TeacherGroupService groupService,
                                  UserService userService,
                                  TeacherRealtimeService realtimeService) {
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.invitationService = invitationService;
        this.groupService = groupService;
        this.userService = userService;
        this.realtimeService = realtimeService;
    }

    @Transactional(readOnly = true)
    public List<GroupStudentTableRowResponse> listStudents(String teacherUid, Long groupId, TeacherStudentSummaryService summaryService) {
        TeacherGroup group = groupService.requireOwnedGroup(teacherUid, groupId);
        return membershipRepository.findByGroupAndStatusOrderByJoinedAtAsc(group, GroupMembershipStatus.ACTIVE)
                .stream()
                .map(membership -> summaryService.toStudentRow(group, membership))
                .toList();
    }

    @Transactional
    public UpdateGroupStudentResponse updateStudent(String teacherUid, Long groupId, Long studentId, UpdateGroupStudentRequest request) {
        TeacherGroup group = groupService.requireOwnedGroup(teacherUid, groupId);
        GroupMembership membership = requireActiveMembership(group, studentId);
        membership.setDisplayNameOverride(trimRequired(request.displayName()));
        membership.setEmailOverride(trimRequired(request.email()).toLowerCase());
        membershipRepository.save(membership);
        publishGroupStudentEvent(group, membership, "group.student_updated");
        return new UpdateGroupStudentResponse(true, "Dane ucznia zostały zaktualizowane.");
    }

    @Transactional
    public RemoveGroupStudentResponse removeStudent(String teacherUid, Long groupId, Long studentId) {
        TeacherGroup group = groupService.requireOwnedGroup(teacherUid, groupId);
        GroupMembership membership = requireActiveMembership(group, studentId);
        membership.setStatus(GroupMembershipStatus.REMOVED);
        membership.setRemovedAt(Instant.now());
        membershipRepository.save(membership);
        publishGroupStudentEvent(group, membership, "group.student_removed");
        return new RemoveGroupStudentResponse(true, "Uczeń został usunięty z grupy.");
    }

    @Transactional
    public AcceptGroupInvitationResponse accept(String studentUid, String clerkEmail, String clerkName, AcceptGroupInvitationRequest request) {
        GroupInvitation invitation = invitationService.requirePendingByRawToken(request.token());
        String effectiveEmail = resolveClerkEmail(studentUid, clerkEmail);

        if (invitation.getEmail() != null && !invitation.getEmail().isBlank()
                && (effectiveEmail == null || !invitation.getEmail().equalsIgnoreCase(effectiveEmail))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "To zaproszenie jest przypisane do innego adresu email.");
        }

        Student student = userService.findOrCreateInvitedStudent(studentUid, effectiveEmail, clerkName);
        if (student.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nie możesz zaakceptować zaproszenia jako nauczyciel.");
        }

        if (invitation.getEmail() != null && !invitation.getEmail().isBlank()
                && !invitation.getEmail().equalsIgnoreCase(student.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ten link zaproszeniowy jest przypisany do innego adresu email.");
        }

        TeacherGroup group = invitation.getGroup();
        GroupMembership membership = membershipRepository.findByGroupAndStudent(group, student)
                .orElseGet(GroupMembership::new);
        membership.setGroup(group);
        membership.setStudent(student);
        membership.setDisplayNameOverride(student.getName());
        membership.setEmailOverride(student.getEmail());
        membership.setStatus(GroupMembershipStatus.ACTIVE);
        membership.setRemovedAt(null);
        membershipRepository.save(membership);

        invitation.setStatus(GroupInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitation.setAcceptedBy(student);
        invitationRepository.save(invitation);
        publishGroupStudentEvent(group, membership, "group.student_joined");

        return new AcceptGroupInvitationResponse(true, "Dołączyłeś do grupy.", group.getId(), group.getName());
    }

    @Transactional(readOnly = true)
    public GroupMembership requireActiveMembership(TeacherGroup group, Long studentId) {
        return membershipRepository.findByGroupIdAndStudentIdAndStatus(group.getId(), studentId, GroupMembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Uczeń nie należy do tej grupy."));
    }

    private String trimRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole jest wymagane.");
        }
        return value.trim();
    }

    private String resolveClerkEmail(String studentUid, String clerkEmail) {
        if (clerkEmail != null && !clerkEmail.isBlank()) {
            return clerkEmail.trim().toLowerCase();
        }
        return userService.findStudentByUid(studentUid)
                .map(Student::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(email -> email.trim().toLowerCase())
                .orElse(null);
    }

    private void publishGroupStudentEvent(TeacherGroup group, GroupMembership membership, String eventName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("groupId", group.getId());
        payload.put("groupName", group.getName());
        payload.put("studentId", membership.getStudent().getId());
        payload.put("studentName", membership.getStudent().getName());
        payload.put("email", membership.getStudent().getEmail());
        payload.values().removeIf(Objects::isNull);

        realtimeService.publishToTeacher(
                group.getTeacher().getClerkUserId(),
                eventName,
                TeacherRealtimeEvent.of(eventName, payload)
        );
    }
}
