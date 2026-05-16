package com.notus.backend.teachergroups;

import com.notus.backend.auth.HashService;
import com.notus.backend.realtime.TeacherRealtimeService;
import com.notus.backend.teachergroups.dto.*;
import com.notus.backend.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherGroupsServicesTest {

    @Mock
    private TeacherGroupRepository groupRepository;
    @Mock
    private GroupMembershipRepository membershipRepository;
    @Mock
    private GroupInvitationRepository invitationRepository;
    @Mock
    private UserService userService;
    @Mock
    private EmailService emailService;
    @Mock
    private GroupInvitationService invitationService;
    @Mock
    private StudentRepository studentRepository;

    private Teacher teacher;
    private Student student;
    private TeacherGroup group;

    @BeforeEach
    void setUp() {
        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setClerkUserId("teacher-uid");
        teacher.setEmail("teacher@example.com");
        teacher.setName("Teacher");
        teacher.setRole(Role.TEACHER);

        student = new Student();
        student.setId(10L);
        student.setClerkUserId("student-uid");
        student.setEmail("student@example.com");
        student.setName("Anna Kowalska");
        student.setRole(Role.STUDENT);

        group = new TeacherGroup();
        group.setId(5L);
        group.setTeacher(teacher);
        group.setName("Matematyka 1A");
        group.setSubject("Matematyka");
        group.setActive(true);
        group.setCreatedAt(Instant.now());
    }

    @Test
    void teacherCreatesGroupAssignedToCurrentTeacher() {
        TeacherGroupService service = new TeacherGroupService(groupRepository, membershipRepository, userService);
        when(userService.findTeacherByUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(groupRepository.save(any(TeacherGroup.class))).thenAnswer(invocation -> {
            TeacherGroup saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        when(membershipRepository.countByGroupAndStatus(any(), eq(GroupMembershipStatus.ACTIVE))).thenReturn(0L);

        TeacherGroupResponse response = service.create("teacher-uid", new CreateTeacherGroupRequest(
                "Matematyka 1A",
                "Opis",
                "Matematyka",
                "2025/2026",
                "2"
        ));

        ArgumentCaptor<TeacherGroup> captor = ArgumentCaptor.forClass(TeacherGroup.class);
        verify(groupRepository).save(captor.capture());
        assertEquals(teacher, captor.getValue().getTeacher());
        assertEquals("Matematyka 1A", response.name());
    }

    @Test
    void teacherCannotAccessForeignGroup() {
        TeacherGroupService service = new TeacherGroupService(groupRepository, membershipRepository, userService);
        when(userService.findTeacherByUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(groupRepository.findByIdAndTeacherAndActiveTrue(99L, teacher)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.getDetails("teacher-uid", 99L));
    }

    @Test
    void invitationStoresOnlyHashAndSendsRawTokenInEmailLink() {
        TeacherGroupService groupService = mock(TeacherGroupService.class);
        GroupInvitationService service = new GroupInvitationService(
                invitationRepository,
                groupService,
                new HashService(),
                emailService,
                studentRepository,
                membershipRepository,
                "http://localhost:5173"
        );
        when(groupService.requireOwnedGroup("teacher-uid", 5L)).thenReturn(group);
        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.save(any(GroupInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InviteStudentResponse response = service.invite("teacher-uid", 5L, new InviteStudentRequest("student@example.com"));

        ArgumentCaptor<GroupInvitation> invitationCaptor = ArgumentCaptor.forClass(GroupInvitation.class);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        verify(emailService).sendGroupInvitation(eq("student@example.com"), eq("Matematyka 1A"), eq("Teacher"), linkCaptor.capture());

        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);
        assertTrue(response.success());
        assertNotEquals(rawToken, invitationCaptor.getValue().getTokenHash());
        assertEquals(new HashService().sha256(rawToken), invitationCaptor.getValue().getTokenHash());
    }

    @Test
    void inviteMarksInvitationFailedWhenEmailProviderFails() {
        TeacherGroupService groupService = mock(TeacherGroupService.class);
        GroupInvitationService service = new GroupInvitationService(
                invitationRepository,
                groupService,
                new HashService(),
                emailService,
                studentRepository,
                membershipRepository,
                "http://localhost:5173"
        );
        when(groupService.requireOwnedGroup("teacher-uid", 5L)).thenReturn(group);
        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.save(any(GroupInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("SMTP rejected")).when(emailService)
                .sendGroupInvitation(eq("student@example.com"), eq("Matematyka 1A"), eq("Teacher"), anyString());

        InviteStudentResponse response = service.invite("teacher-uid", 5L, new InviteStudentRequest("student@example.com"));

        ArgumentCaptor<GroupInvitation> invitationCaptor = ArgumentCaptor.forClass(GroupInvitation.class);
        verify(invitationRepository, atLeastOnce()).save(invitationCaptor.capture());
        assertFalse(response.success());
        assertEquals(GroupInvitationStatus.FAILED, invitationCaptor.getAllValues().getLast().getStatus());
    }

    @Test
    void acceptingInvitationCreatesMembershipAndMarksInvitationAccepted() {
        GroupMembershipService service = new GroupMembershipService(
                membershipRepository,
                invitationRepository,
                invitationService,
                mock(TeacherGroupService.class),
                userService,
                mock(TeacherRealtimeService.class)
        );
        GroupInvitation invitation = new GroupInvitation();
        invitation.setGroup(group);
        invitation.setEmail("student@example.com");
        invitation.setStatus(GroupInvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plusSeconds(3600));
        invitation.setCreatedByTeacher(teacher);

        when(invitationService.requirePendingByRawToken("raw-token")).thenReturn(invitation);
        when(userService.findOrCreateInvitedStudent("student-uid", "student@example.com", "Anna Kowalska")).thenReturn(student);
        when(membershipRepository.findByGroupAndStudent(group, student)).thenReturn(Optional.empty());
        when(membershipRepository.save(any(GroupMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcceptGroupInvitationResponse response = service.accept(
                "student-uid",
                "student@example.com",
                "Anna Kowalska",
                new AcceptGroupInvitationRequest("raw-token")
        );

        ArgumentCaptor<GroupMembership> membershipCaptor = ArgumentCaptor.forClass(GroupMembership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        verify(invitationRepository).save(invitation);
        assertTrue(response.success());
        assertEquals(GroupMembershipStatus.ACTIVE, membershipCaptor.getValue().getStatus());
        assertEquals(GroupInvitationStatus.ACCEPTED, invitation.getStatus());
        assertEquals(student, invitation.getAcceptedBy());
    }

    @Test
    void removingStudentSoftDeletesMembership() {
        TeacherGroupService groupService = mock(TeacherGroupService.class);
        GroupMembershipService service = new GroupMembershipService(
                membershipRepository,
                invitationRepository,
                invitationService,
                groupService,
                userService,
                mock(TeacherRealtimeService.class)
        );
        GroupMembership membership = new GroupMembership();
        membership.setGroup(group);
        membership.setStudent(student);
        membership.setStatus(GroupMembershipStatus.ACTIVE);

        when(groupService.requireOwnedGroup("teacher-uid", 5L)).thenReturn(group);
        when(membershipRepository.findByGroupIdAndStudentIdAndStatus(5L, 10L, GroupMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));

        RemoveGroupStudentResponse response = service.removeStudent("teacher-uid", 5L, 10L);

        assertTrue(response.success());
        assertEquals(GroupMembershipStatus.REMOVED, membership.getStatus());
        assertNotNull(membership.getRemovedAt());
        verify(membershipRepository).save(membership);
        verifyNoInteractions(userService);
    }

    @Test
    void listGroupsReturnsOnlyTeacherGroupsFromRepository() {
        TeacherGroupService service = new TeacherGroupService(groupRepository, membershipRepository, userService);
        when(userService.findTeacherByUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(groupRepository.findByTeacherAndActiveTrueOrderByCreatedAtDesc(teacher)).thenReturn(List.of(group));
        when(membershipRepository.countByGroupAndStatus(group, GroupMembershipStatus.ACTIVE)).thenReturn(3L);

        List<TeacherGroupResponse> result = service.listGroups("teacher-uid");

        assertEquals(1, result.size());
        assertEquals(3L, result.getFirst().studentsCount());
        verify(groupRepository).findByTeacherAndActiveTrueOrderByCreatedAtDesc(teacher);
    }
}
