package com.notus.backend.auth;

import com.notus.backend.attendance.group.StudentGroupRepository;
import com.notus.backend.auth.dto.LoginRequest;
import com.notus.backend.auth.dto.StudentRegisterRequest;
import com.notus.backend.auth.dto.TeacherAuthResponse;
import com.notus.backend.users.Role;
import com.notus.backend.users.Student;
import com.notus.backend.users.StudentRepository;
import com.notus.backend.users.TeacherRepository;
import com.notus.backend.users.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class StudentRegistrationService {

    private final LocalAuthUserRepository localAuthUserRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public StudentRegistrationService(LocalAuthUserRepository localAuthUserRepository,
                                      StudentRepository studentRepository,
                                      TeacherRepository teacherRepository,
                                      StudentGroupRepository studentGroupRepository,
                                      PasswordEncoder passwordEncoder,
                                      AuthTokenService authTokenService) {
        this.localAuthUserRepository = localAuthUserRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    @Transactional
    public TeacherAuthResponse register(StudentRegisterRequest request) {
        String email = normalizeEmail(request.email());
        validateEmail(email);
        validatePassword(request.password());
        ensureStudentCanBeCreated(email);

        String authUserId = "local-" + UUID.randomUUID();
        LocalAuthUser authUser = new LocalAuthUser();
        authUser.setAuthUserId(authUserId);
        authUser.setEmail(email);
        authUser.setPasswordHash(passwordEncoder.encode(request.password()));
        authUser.setEmailVerified(true);
        localAuthUserRepository.save(authUser);

        Student student = createStudent(authUserId, email, request.name());
        return new TeacherAuthResponse(
                authTokenService.issueLocalToken(authUserId, email),
                mapStudent(student),
                true,
                false,
                "Zarejestrowano ucznia."
        );
    }

    @Transactional(readOnly = true)
    public TeacherAuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        LocalAuthUser authUser = localAuthUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nieprawidłowy email lub hasło"));

        if (!passwordEncoder.matches(request.password(), authUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nieprawidłowy email lub hasło");
        }

        Student student = studentRepository.findByClerkUserId(authUser.getAuthUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "To konto nie jest kontem ucznia"));

        return new TeacherAuthResponse(
                authTokenService.issueLocalToken(authUser.getAuthUserId(), authUser.getEmail()),
                mapStudent(student),
                true,
                false,
                "Zalogowano."
        );
    }

    private Student createStudent(String authUserId, String email, String name) {
        Student student = new Student();
        student.setClerkUserId(authUserId);
        student.setEmail(email);
        student.setName(resolveName(email, name));
        student.setRole(Role.STUDENT);
        student.setIndexNumber(resolveIndexNumber(email));
        studentGroupRepository.findByCode("INF-2024-SEM2")
                .ifPresent(group -> student.setStudentGroups(List.of(group)));
        return studentRepository.save(student);
    }

    private void ensureStudentCanBeCreated(String email) {
        if (teacherRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "To konto istnieje już jako nauczyciel.");
        }

        if (studentRepository.findByEmailIgnoreCase(email).isPresent() || localAuthUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ten email jest już zajęty.");
        }
    }

    private UserDto mapStudent(Student student) {
        return new UserDto(
                student.getId(),
                student.getEmail(),
                student.getName(),
                student.getRole(),
                student.getIndexNumber()
        );
    }

    private void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podaj poprawny adres email.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8
                || !password.chars().anyMatch(Character::isDigit)
                || !password.chars().anyMatch(Character::isUpperCase)
                || !password.chars().anyMatch(Character::isLowerCase)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hasło jest zbyt słabe.");
        }
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
        return email != null && email.contains("@") ? email.substring(0, email.indexOf("@")) : "Student";
    }

    private String resolveIndexNumber(String email) {
        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf("@"));
            if (!localPart.isBlank()) {
                return localPart;
            }
        }
        return null;
    }
}
