package com.notus.backend.teachergroups;

import com.notus.backend.teachergroups.dto.*;
import com.notus.backend.users.Role;
import com.notus.backend.users.Student;
import com.notus.backend.users.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class GroupMembershipService {

    private final GroupMembershipRepository membershipRepository;
    private final GroupInvitationRepository invitationRepository;
    private final GroupInvitationService invitationService;
    private final TeacherGroupService groupService;
    private final UserService userService;

    public GroupMembershipService(GroupMembershipRepository membershipRepository,
                                  GroupInvitationRepository invitationRepository,
                                  GroupInvitationService invitationService,
                                  TeacherGroupService groupService,
                                  UserService userService) {
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.invitationService = invitationService;
        this.groupService = groupService;
        this.userService = userService;
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
        return new UpdateGroupStudentResponse(true, "Dane ucznia zostały zaktualizowane.");
    }

    @Transactional
    public RemoveGroupStudentResponse removeStudent(String teacherUid, Long groupId, Long studentId) {
        TeacherGroup group = groupService.requireOwnedGroup(teacherUid, groupId);
        GroupMembership membership = requireActiveMembership(group, studentId);
        membership.setStatus(GroupMembershipStatus.REMOVED);
        membership.setRemovedAt(Instant.now());
        membershipRepository.save(membership);
        return new RemoveGroupStudentResponse(true, "Uczeń został usunięty z grupy.");
    }

    @Transactional
    public AcceptGroupInvitationResponse accept(String studentUid, AcceptGroupInvitationRequest request) {
        Student student = userService.findStudentByUid(studentUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Nie możesz zaakceptować zaproszenia jako nauczyciel."));
        if (student.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nie możesz zaakceptować zaproszenia jako nauczyciel.");
        }

        GroupInvitation invitation = invitationService.requirePendingByRawToken(request.token());
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
}
