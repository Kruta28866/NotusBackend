package com.notus.backend.users;

import com.notus.backend.users.teachercode.TeacherCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherCodeService teacherCodeService;

    @Mock
    private AppUserIdentityService appUserIdentityService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(studentRepository, teacherRepository, teacherCodeService, appUserIdentityService);
    }

    @Test
    void existingTeacherWithSubmittedCodeStillValidatesCode() {
        Teacher teacher = teacher("dev-teacher", "teacher@example.com");
        when(teacherRepository.findByClerkUserId("dev-teacher")).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(teacher)).thenReturn(teacher);

        UserDto result = userService.findOrCreate(
                "dev-teacher",
                "teacher@example.com",
                "Teacher",
                Role.TEACHER,
                "WRONG-CODE"
        );

        verify(teacherCodeService).validateCode("WRONG-CODE");
        assertEquals(Role.TEACHER, result.role());
    }

    @Test
    void existingUserCannotSwitchRoleDuringLogin() {
        Student student = student("dev-student", "student@example.com");
        when(teacherRepository.findByClerkUserId("dev-student")).thenReturn(Optional.empty());
        when(studentRepository.findByClerkUserId("dev-student")).thenReturn(Optional.of(student));

        assertThrows(ResponseStatusException.class, () -> userService.findOrCreate(
                "dev-student",
                "student@example.com",
                "Student",
                Role.TEACHER,
                "VALID-CODE"
        ));
    }

    @Test
    void existingTeacherIsResolvedByEmailWhenProviderSubjectDiffers() {
        Teacher teacher = teacher("local-teacher", "teacher@example.com");
        when(teacherRepository.findByClerkUserId("google-teacher")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(teacher)).thenReturn(teacher);

        UserDto result = userService.findOrCreate("google-teacher", "Teacher@Example.com", "Teacher");

        assertEquals(Role.TEACHER, result.role());
        assertEquals("teacher@example.com", result.email());
    }

    @Test
    void studentRegistrationStoresPhoneNumber() {
        when(teacherRepository.findByClerkUserId("student-uid")).thenReturn(Optional.empty());
        when(studentRepository.findByClerkUserId("student-uid")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.empty());
        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        UserDto result = userService.findOrCreate(
                "student-uid",
                "student@example.com",
                "Student",
                Role.STUDENT,
                null,
                "+48 123 456 789"
        );

        assertEquals(Role.STUDENT, result.role());
        assertEquals("+48 123 456 789", result.phoneNumber());
        verify(studentRepository).save(any(Student.class));
        verify(appUserIdentityService).ensureForStudent(any(Student.class));
    }

    private Teacher teacher(String clerkUserId, String email) {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setClerkUserId(clerkUserId);
        teacher.setEmail(email);
        teacher.setName("Teacher");
        teacher.setRole(Role.TEACHER);
        return teacher;
    }

    private Student student(String clerkUserId, String email) {
        Student student = new Student();
        student.setId(1L);
        student.setClerkUserId(clerkUserId);
        student.setEmail(email);
        student.setName("Student");
        student.setRole(Role.STUDENT);
        return student;
    }
}
