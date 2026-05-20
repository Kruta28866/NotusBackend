package com.notus.backend.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.notus.backend.auth.dto.LoginRequest;
import com.notus.backend.auth.dto.TeacherAuthResponse;
import com.notus.backend.auth.dto.TeacherGoogleRegisterRequest;
import com.notus.backend.auth.dto.TeacherRegisterRequest;
import com.notus.backend.users.AppUserIdentityService;
import com.notus.backend.users.Role;
import com.notus.backend.users.Student;
import com.notus.backend.users.StudentRepository;
import com.notus.backend.users.Teacher;
import com.notus.backend.users.TeacherRepository;
import com.notus.backend.users.teachercode.TeacherCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherRegistrationServiceTest {

    @Mock
    private TeacherCodeService teacherCodeService;
    @Mock
    private LocalAuthUserRepository localAuthUserRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private AppUserIdentityService appUserIdentityService;

    private TeacherRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new TeacherRegistrationService(
                teacherCodeService,
                localAuthUserRepository,
                teacherRepository,
                studentRepository,
                passwordEncoder,
                new AuthTokenService("test-secret"),
                new HashService(),
                emailVerificationService,
                appUserIdentityService
        );
    }

    @Test
    void emailRegistrationCreatesTeacherAndConsumesCodeAfterSave() {
        when(studentRepository.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.empty());
        when(localAuthUserRepository.existsByEmailIgnoreCase("teacher@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("hashed");
        when(localAuthUserRepository.save(any(LocalAuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> {
            Teacher teacher = invocation.getArgument(0);
            teacher.setId(7L);
            return teacher;
        });

        TeacherAuthResponse response = service.registerWithEmail(new TeacherRegisterRequest(
                "registration-token",
                "Jan Kowalski",
                "teacher@example.com",
                "StrongPass1"
        ));

        assertTrue(response.requiresEmailVerification());
        assertFalse(response.emailVerified());
        assertNull(response.token());
        InOrder inOrder = inOrder(localAuthUserRepository, teacherRepository, teacherCodeService, emailVerificationService);
        inOrder.verify(localAuthUserRepository).save(any(LocalAuthUser.class));
        inOrder.verify(teacherRepository).save(any(Teacher.class));
        inOrder.verify(teacherCodeService).consumeRegistrationToken(eq("registration-token"), eq("teacher@example.com"), any());
        inOrder.verify(emailVerificationService).sendVerificationEmail(eq("teacher@example.com"), any());
    }

    @Test
    void emailRegistrationDoesNotConsumeCodeWhenEmailBelongsToStudent() {
        Student student = new Student();
        student.setRole(Role.STUDENT);
        when(studentRepository.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.of(student));

        assertThrows(ResponseStatusException.class, () -> service.registerWithEmail(new TeacherRegisterRequest(
                "registration-token",
                "Jan Kowalski",
                "teacher@example.com",
                "StrongPass1"
        )));

        verify(teacherCodeService, never()).consumeRegistrationToken(any(), any(), any());
    }

    @Test
    void loginRequiresVerifiedEmailBeforeIssuingToken() {
        LocalAuthUser authUser = new LocalAuthUser();
        authUser.setAuthUserId("local-1");
        authUser.setEmail("teacher@example.com");
        authUser.setPasswordHash("hashed");
        authUser.setEmailVerified(false);
        Teacher teacher = teacher("local-1", "teacher@example.com");

        when(localAuthUserRepository.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("StrongPass1", "hashed")).thenReturn(true);
        when(teacherRepository.findByClerkUserId("local-1")).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(teacher)).thenReturn(teacher);

        TeacherAuthResponse response = service.login(new LoginRequest("teacher@example.com", "StrongPass1"));

        assertTrue(response.requiresEmailVerification());
        assertNull(response.token());
    }

    @Test
    void googleRegistrationRejectsExistingStudentAndDoesNotConsumeCode() {
        String idToken = JWT.create()
                .withSubject("google-user-1")
                .withClaim("email", "student@example.com")
                .withClaim("email_verified", true)
                .sign(Algorithm.HMAC256("test"));

        when(teacherRepository.findByClerkUserId("google-user-1")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.empty());
        when(studentRepository.findByClerkUserId("google-user-1")).thenReturn(Optional.empty());
        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(new Student()));

        assertThrows(ResponseStatusException.class, () -> service.registerOrLoginWithGoogle(
                new TeacherGoogleRegisterRequest(idToken, "registration-token")
        ));

        verify(teacherCodeService, never()).consumeRegistrationToken(any(), any(), any());
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
}
