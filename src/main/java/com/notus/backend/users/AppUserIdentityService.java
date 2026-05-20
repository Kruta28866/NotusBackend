package com.notus.backend.users;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Service
public class AppUserIdentityService {

    private final AppUserRepository appUserRepository;

    public AppUserIdentityService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUser ensureForStudent(Student student) {
        return ensure(
                student.getClerkUserId(),
                student.getEmail(),
                student.getName(),
                Role.STUDENT,
                student.getPhoneNumber()
        );
    }

    @Transactional
    public AppUser ensureForTeacher(Teacher teacher) {
        return ensure(
                teacher.getClerkUserId(),
                teacher.getEmail(),
                teacher.getName(),
                Role.TEACHER,
                null
        );
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByIdentity(String authUserId, String email) {
        if (hasText(authUserId)) {
            Optional<AppUser> byAuthUserId = appUserRepository.findByAuthUserId(authUserId);
            if (byAuthUserId.isPresent()) {
                return byAuthUserId;
            }
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        return appUserRepository.findByEmailIgnoreCase(normalizedEmail);
    }

    private AppUser ensure(String authUserId, String email, String name, Role role, String phoneNumber) {
        if (!hasText(authUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak identyfikatora użytkownika.");
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak adresu email użytkownika.");
        }

        Optional<AppUser> existing = appUserRepository.findByAuthUserId(authUserId)
                .or(() -> appUserRepository.findByEmailIgnoreCase(normalizedEmail));

        AppUser user = existing.orElseGet(AppUser::new);

        if (user.getId() != null && user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "To konto istnieje już jako " + user.getRole());
        }

        appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(other -> user.getId() != null && !other.getId().equals(user.getId()))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ten email jest już zajęty.");
                });

        if (user.getId() == null) {
            user.setAuthUserId(authUserId);
            user.setRole(role);
        }

        user.setEmail(normalizedEmail);
        user.setName(resolveName(normalizedEmail, name));
        user.setPhoneNumber(role == Role.STUDENT ? phoneNumber : null);
        return appUserRepository.save(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveName(String email, String name) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return email != null && email.contains("@") ? email.substring(0, email.indexOf("@")) : "User";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
