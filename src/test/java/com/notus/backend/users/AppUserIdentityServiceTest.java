package com.notus.backend.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserIdentityServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    private AppUserIdentityService service;

    @BeforeEach
    void setUp() {
        service = new AppUserIdentityService(appUserRepository);
    }

    @Test
    void createsCanonicalUserForStudentProfile() {
        Student student = student("student-auth-1", "Student@Example.com", "Anna Kowalska");
        student.setPhoneNumber("+48 123 456 789");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        AppUser result = service.ensureForStudent(student);

        assertEquals(10L, result.getId());
        assertEquals("student-auth-1", result.getAuthUserId());
        assertEquals("student@example.com", result.getEmail());
        assertEquals("Anna Kowalska", result.getName());
        assertEquals(Role.STUDENT, result.getRole());
        assertEquals("+48 123 456 789", result.getPhoneNumber());
    }

    @Test
    void keepsExistingAuthIdentityWhenProfileIsResolvedByEmail() {
        Student student = student("google-subject", "student@example.com", "Anna Nowak");
        AppUser existing = appUser(3L, "local-subject", "student@example.com", "Anna Kowalska", Role.STUDENT);
        when(appUserRepository.findByAuthUserId("google-subject")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = service.ensureForStudent(student);

        assertEquals("local-subject", result.getAuthUserId());
        assertEquals("Anna Nowak", result.getName());
        assertEquals(Role.STUDENT, result.getRole());
    }

    @Test
    void rejectsRoleConflictForSameIdentity() {
        Student student = student("auth-1", "person@example.com", "Person");
        AppUser existing = appUser(1L, "auth-1", "person@example.com", "Person", Role.TEACHER);
        when(appUserRepository.findByAuthUserId("auth-1")).thenReturn(Optional.of(existing));

        assertThrows(ResponseStatusException.class, () -> service.ensureForStudent(student));
    }

    @Test
    void findsByAuthUserIdBeforeEmail() {
        AppUser existing = appUser(1L, "auth-1", "student@example.com", "Student", Role.STUDENT);
        when(appUserRepository.findByAuthUserId("auth-1")).thenReturn(Optional.of(existing));

        Optional<AppUser> result = service.findByIdentity("auth-1", "other@example.com");

        assertEquals(Optional.of(existing), result);
    }

    private Student student(String authUserId, String email, String name) {
        Student student = new Student();
        student.setClerkUserId(authUserId);
        student.setEmail(email);
        student.setName(name);
        student.setRole(Role.STUDENT);
        return student;
    }

    private AppUser appUser(Long id, String authUserId, String email, String name, Role role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setAuthUserId(authUserId);
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        return user;
    }
}
