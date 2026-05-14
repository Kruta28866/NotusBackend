package com.notus.backend.users;

import com.notus.backend.attendance.group.StudentGroupRepository;
import com.notus.backend.users.teachercode.TeacherCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentGroupRepository studentGroupRepository;

    @Mock
    private TeacherCodeService teacherCodeService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(studentRepository, teacherRepository, studentGroupRepository, teacherCodeService);
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
