package com.notus.backend.auth;

import com.notus.backend.auth.dto.LoginRequest;
import com.notus.backend.auth.dto.StudentRegisterRequest;
import com.notus.backend.auth.dto.TeacherAuthResponse;
import com.notus.backend.users.AppUserIdentityService;
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

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class StudentRegistrationService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s()\\-]{6,24}$");

    private final LocalAuthUserRepository localAuthUserRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final AppUserIdentityService appUserIdentityService;

    public StudentRegistrationService(LocalAuthUserRepository localAuthUserRepository,
                                      StudentRepository studentRepository,
                                      TeacherRepository teacherRepository,
                                      PasswordEncoder passwordEncoder,
                                      AuthTokenService authTokenService,
                                      AppUserIdentityService appUserIdentityService) {
        this.localAuthUserRepository = localAuthUserRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.appUserIdentityService = appUserIdentityService;
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

        Student student = createStudent(authUserId, email, request.name(), request.phoneNumber());
        return new TeacherAuthResponse(
                authTokenService.issueLocalToken(authUserId, email),
                mapStudent(student),
                true,
                false,
                "Zarejestrowano ucznia."
        );
    }

    @Transactional
    public TeacherAuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        LocalAuthUser authUser = localAuthUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nieprawidłowy email lub hasło"));

        if (!passwordEncoder.matches(request.password(), authUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nieprawidłowy email lub hasło");
        }

        Student student = studentRepository.findByClerkUserId(authUser.getAuthUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "To konto nie jest kontem ucznia"));
        student.setUser(appUserIdentityService.ensureForStudent(student));
        student = studentRepository.save(student);

        return new TeacherAuthResponse(
                authTokenService.issueLocalToken(authUser.getAuthUserId(), authUser.getEmail()),
                mapStudent(student),
                true,
                false,
                "Zalogowano."
        );
    }

    private Student createStudent(String authUserId, String email, String name, String phoneNumber) {
        Student student = new Student();
        student.setClerkUserId(authUserId);
        student.setEmail(email);
        student.setName(resolveName(email, name));
        student.setRole(Role.STUDENT);
        student.setIndexNumber(resolveIndexNumber(email));
        student.setPhoneNumber(normalizePhone(phoneNumber));
        student.setUser(appUserIdentityService.ensureForStudent(student));
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
                student.getIndexNumber(),
                student.getPhoneNumber()
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

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        String trimmed = phoneNumber.trim();
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podaj poprawny numer telefonu.");
        }
        return trimmed;
    }
}
