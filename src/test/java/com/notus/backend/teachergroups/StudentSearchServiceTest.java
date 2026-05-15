package com.notus.backend.teachergroups;

import com.notus.backend.teachergroups.dto.StudentSearchResponse;
import com.notus.backend.users.Role;
import com.notus.backend.users.Student;
import com.notus.backend.users.StudentRepository;
import com.notus.backend.users.Teacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentSearchServiceTest {

    @Mock
    private TeacherGroupService groupService;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private GroupMembershipRepository membershipRepository;

    private StudentSearchService service;
    private TeacherGroup group;
    private Student anna;
    private Student annaInGroup;

    @BeforeEach
    void setUp() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setClerkUserId("teacher-uid");
        teacher.setRole(Role.TEACHER);

        group = new TeacherGroup();
        group.setId(5L);
        group.setTeacher(teacher);
        group.setActive(true);

        anna = student(10L, "Anna Kowalska", "anna@example.com");
        annaInGroup = student(11L, "Anna Nowak", "anna.nowak@example.com");

        service = new StudentSearchService(groupService, studentRepository, membershipRepository);
    }

    @Test
    void findsStudentsByPartialEmailAndMarksMembership() {
        when(groupService.requireOwnedGroup("teacher-uid", 5L)).thenReturn(group);
        when(studentRepository.findByRoleAndEmailContainingIgnoreCaseOrderByEmailAsc(eq(Role.STUDENT), eq("anna"), any(Pageable.class)))
                .thenReturn(List.of(anna, annaInGroup));
        when(membershipRepository.existsByGroupAndStudentAndStatus(group, anna, GroupMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.existsByGroupAndStudentAndStatus(group, annaInGroup, GroupMembershipStatus.ACTIVE)).thenReturn(true);

        List<StudentSearchResponse> result = service.search("teacher-uid", 5L, " anna ");

        assertEquals(2, result.size());
        assertEquals("Anna Kowalska", result.get(0).fullName());
        assertFalse(result.get(0).alreadyInGroup());
        assertTrue(result.get(1).alreadyInGroup());
    }

    @Test
    void trimsAndLowercasesQuery() {
        when(groupService.requireOwnedGroup("teacher-uid", 5L)).thenReturn(group);
        when(studentRepository.findByRoleAndEmailContainingIgnoreCaseOrderByEmailAsc(eq(Role.STUDENT), eq("ann"), any(Pageable.class)))
                .thenReturn(List.of());

        service.search("teacher-uid", 5L, "  Ann  ");

        verify(studentRepository).findByRoleAndEmailContainingIgnoreCaseOrderByEmailAsc(eq(Role.STUDENT), eq("ann"), any(Pageable.class));
    }

    @Test
    void requiresAtLeastThreeCharacters() {
        when(groupService.requireOwnedGroup("teacher-uid", 5L)).thenReturn(group);

        List<StudentSearchResponse> result = service.search("teacher-uid", 5L, "an");

        assertTrue(result.isEmpty());
        verifyNoInteractions(studentRepository);
    }

    @Test
    void searchesOnlyStudentsAndLimitsResultsToTen() {
        when(groupService.requireOwnedGroup("teacher-uid", 5L)).thenReturn(group);
        when(studentRepository.findByRoleAndEmailContainingIgnoreCaseOrderByEmailAsc(eq(Role.STUDENT), eq("ann"), any(Pageable.class)))
                .thenReturn(List.of());

        service.search("teacher-uid", 5L, "ann");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(studentRepository).findByRoleAndEmailContainingIgnoreCaseOrderByEmailAsc(
                eq(Role.STUDENT),
                eq("ann"),
                pageableCaptor.capture()
        );
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void teacherCannotSearchForeignGroup() {
        when(groupService.requireOwnedGroup("teacher-uid", 99L)).thenThrow(ResponseStatusException.class);

        assertThrows(ResponseStatusException.class, () -> service.search("teacher-uid", 99L, "anna"));
        verifyNoInteractions(studentRepository);
    }

    private Student student(Long id, String name, String email) {
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setEmail(email);
        student.setClerkUserId("student-" + id);
        student.setRole(Role.STUDENT);
        return student;
    }
}
