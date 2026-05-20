package com.notus.backend.users;

import com.notus.backend.users.teachercode.TeacherCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s()\\-]{6,24}$");

    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final TeacherCodeService teacherCodeService;
    private final AppUserIdentityService appUserIdentityService;

    public UserService(StudentRepository studentRepo,
                       TeacherRepository teacherRepo,
                       TeacherCodeService teacherCodeService,
                       AppUserIdentityService appUserIdentityService) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.teacherCodeService = teacherCodeService;
        this.appUserIdentityService = appUserIdentityService;
    }

    @Transactional
    public UserDto findOrCreate(String clerkUserId, String email, String name) {
        String normalizedEmail = normalizeEmail(email);

        Optional<Teacher> existingTeacher = teacherRepo.findByClerkUserId(clerkUserId)
                .or(() -> normalizedEmail == null ? Optional.empty() : teacherRepo.findByEmailIgnoreCase(normalizedEmail));
        if (existingTeacher.isPresent()) {
            Teacher teacher = existingTeacher.get();
            updateTeacherData(teacher, normalizedEmail, name);
            teacher = saveTeacherWithIdentity(teacher);
            return mapTeacherToDto(teacher);
        }

        Optional<Student> existingStudent = studentRepo.findByClerkUserId(clerkUserId)
                .or(() -> normalizedEmail == null ? Optional.empty() : studentRepo.findByEmailIgnoreCase(normalizedEmail));
        if (existingStudent.isPresent()) {
            Student student = existingStudent.get();
            updateStudentData(student, normalizedEmail, name, null);
            student = saveStudentWithIdentity(student);
            return mapStudentToDto(student);
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Wybierz typ konta przed rejestracją");
    }

    @Transactional
    public UserDto findOrCreate(String clerkUserId, String email, String name, Role requestedRole, String providedTeacherAccessCode) {
        return findOrCreate(clerkUserId, email, name, requestedRole, providedTeacherAccessCode, null);
    }

    @Transactional
    public UserDto findOrCreate(String clerkUserId,
                                String email,
                                String name,
                                Role requestedRole,
                                String providedTeacherAccessCode,
                                String phoneNumber) {
        String normalizedEmail = normalizeEmail(email);
        Optional<UserDto> existingUser = findExistingByIdentity(clerkUserId, normalizedEmail);
        if (existingUser.isPresent()) {
            UserDto user = existingUser.get();

            if (requestedRole != null && user.role() != requestedRole) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "To konto jest już zarejestrowane jako " + user.role());
            }

            if (user.role() == Role.TEACHER && requestedRole == Role.TEACHER && hasText(providedTeacherAccessCode)) {
                teacherCodeService.validateCode(providedTeacherAccessCode);
            }

            if (user.role() == Role.STUDENT) {
                Student student = studentRepo.findByClerkUserId(clerkUserId)
                        .or(() -> normalizedEmail == null ? Optional.empty() : studentRepo.findByEmailIgnoreCase(normalizedEmail))
                        .orElseThrow();
                updateStudentData(student, normalizedEmail, name, phoneNumber);
                return mapStudentToDto(saveStudentWithIdentity(student));
            }

            return findOrCreate(clerkUserId, normalizedEmail, name);
        }

        if (requestedRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Typ konta jest wymagany");
        }

        if (requestedRole == Role.TEACHER) {
            ensureNoStudentWithEmail(normalizedEmail);
            teacherCodeService.consumeCode(providedTeacherAccessCode);
            Teacher teacher = createTeacher(clerkUserId, normalizedEmail, name);
            return mapTeacherToDto(teacher);
        }

        if (requestedRole == Role.STUDENT) {
            ensureNoTeacherWithEmail(normalizedEmail);
            Student student = createStudent(clerkUserId, normalizedEmail, name, phoneNumber);
            return mapStudentToDto(student);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieobsługiwany typ konta");
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findExistingByUid(String clerkUserId) {
        Optional<Teacher> teacher = teacherRepo.findByClerkUserId(clerkUserId);
        if (teacher.isPresent()) {
            return teacher.map(this::mapTeacherToDto);
        }

        return studentRepo.findByClerkUserId(clerkUserId).map(this::mapStudentToDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findExistingByIdentity(String clerkUserId, String email) {
        Optional<UserDto> byUid = findExistingByUid(clerkUserId);
        if (byUid.isPresent()) {
            return byUid;
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        Optional<Teacher> teacher = teacherRepo.findByEmailIgnoreCase(normalizedEmail);
        if (teacher.isPresent()) {
            return teacher.map(this::mapTeacherToDto);
        }

        return studentRepo.findByEmailIgnoreCase(normalizedEmail).map(this::mapStudentToDto);
    }

    @Transactional(readOnly = true)
    public Optional<String> resolvePrincipalUserId(String clerkUserId, String email) {
        Optional<Teacher> teacherByUid = teacherRepo.findByClerkUserId(clerkUserId);
        if (teacherByUid.isPresent()) {
            return teacherByUid.map(Teacher::getClerkUserId);
        }

        Optional<Student> studentByUid = studentRepo.findByClerkUserId(clerkUserId);
        if (studentByUid.isPresent()) {
            return studentByUid.map(Student::getClerkUserId);
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        Optional<Teacher> teacherByEmail = teacherRepo.findByEmailIgnoreCase(normalizedEmail);
        if (teacherByEmail.isPresent()) {
            return teacherByEmail.map(Teacher::getClerkUserId);
        }

        return studentRepo.findByEmailIgnoreCase(normalizedEmail).map(Student::getClerkUserId);
    }

    public Optional<Student> findStudentByUid(String uid) {
        return studentRepo.findByClerkUserId(uid);
    }

    @Transactional
    public Student findOrCreateInvitedStudent(String clerkUserId, String email, String name) {
        if (clerkUserId == null || clerkUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Brak zalogowanego użytkownika.");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak adresu email zalogowanego użytkownika.");
        }

        Optional<Teacher> teacherByUid = teacherRepo.findByClerkUserId(clerkUserId);
        Optional<Teacher> teacherByEmail = teacherRepo.findByEmailIgnoreCase(email);
        if (teacherByUid.isPresent() || teacherByEmail.isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nie możesz zaakceptować zaproszenia jako nauczyciel.");
        }

        Optional<Student> existingByUid = studentRepo.findByClerkUserId(clerkUserId);
        if (existingByUid.isPresent()) {
            Student student = existingByUid.get();
            updateStudentData(student, email, name, null);
            return saveStudentWithIdentity(student);
        }

        Optional<Student> existingByEmail = studentRepo.findByEmailIgnoreCase(email);
        if (existingByEmail.isPresent()) {
            Student student = existingByEmail.get();
            updateStudentData(student, email, name, null);
            return saveStudentWithIdentity(student);
        }

        return createStudent(clerkUserId, email, name, null);
    }

    public Optional<Student> findStudentWithGroupsByUid(String uid) {
        return studentRepo.findWithStudentGroupsByClerkUserId(uid);
    }

    public Optional<Teacher> findTeacherByUid(String uid) {
        return teacherRepo.findByClerkUserId(uid);
    }

    private Student createStudent(String clerkUserId, String email, String name, String phoneNumber) {
        Student student = new Student();
        student.setClerkUserId(clerkUserId);
        student.setEmail(resolveEmail(clerkUserId, email));
        student.setName(resolveName(email, name));
        student.setRole(Role.STUDENT);
        student.setIndexNumber(resolveIndexNumber(email));
        student.setPhoneNumber(normalizePhone(phoneNumber));
        return saveStudentWithIdentity(student);
    }

    private Teacher createTeacher(String clerkUserId, String email, String name) {
        Teacher teacher = new Teacher();
        teacher.setClerkUserId(clerkUserId);
        teacher.setEmail(resolveEmail(clerkUserId, email));
        teacher.setName(resolveName(email, name));
        teacher.setRole(Role.TEACHER);
        return saveTeacherWithIdentity(teacher);
    }

    private Student saveStudentWithIdentity(Student student) {
        student.setUser(appUserIdentityService.ensureForStudent(student));
        return studentRepo.save(student);
    }

    private Teacher saveTeacherWithIdentity(Teacher teacher) {
        teacher.setUser(appUserIdentityService.ensureForTeacher(teacher));
        return teacherRepo.save(teacher);
    }

    private void updateStudentData(Student student, String email, String name, String phoneNumber) {
        if (email != null && !email.isBlank()) {
            student.setEmail(normalizeEmail(email));
        }

        student.setName(resolveName(student.getEmail(), name));
        student.setIndexNumber(resolveIndexNumber(student.getEmail()));
        String normalizedPhone = normalizePhone(phoneNumber);
        if (normalizedPhone != null) {
            student.setPhoneNumber(normalizedPhone);
        }
    }

    private void updateTeacherData(Teacher teacher, String email, String name) {
        if (email != null && !email.isBlank()) {
            teacher.setEmail(email);
        }

        teacher.setName(resolveName(teacher.getEmail(), name));
    }

    private UserDto mapStudentToDto(Student student) {
        return new UserDto(
                student.getId(),
                student.getEmail(),
                student.getName(),
                student.getRole(),
                student.getIndexNumber(),
                student.getPhoneNumber()
        );
    }

    private UserDto mapTeacherToDto(Teacher teacher) {
        return new UserDto(
                teacher.getId(),
                teacher.getEmail(),
                teacher.getName(),
                teacher.getRole(),
                null,
                null
        );
    }

    private String resolveEmail(String clerkUserId, String email) {
        String normalized = normalizeEmail(email);
        if (normalized != null) {
            return normalized;
        }
        return clerkUserId + "@temporary.com";
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
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

    private void ensureNoTeacherWithEmail(String email) {
        if (email != null && teacherRepo.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "To konto istnieje już jako nauczyciel.");
        }
    }

    private void ensureNoStudentWithEmail(String email) {
        if (email != null && studentRepo.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "To konto istnieje już jako uczeń.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveName(String email, String name) {
        if (name != null && !name.isBlank()) {
            return name;
        }

        if (email != null && email.contains("@")) {
            String localPart = email.split("@")[0];
            if (!localPart.isBlank()) {
                return localPart;
            }
        }

        return "User";
    }

    private String resolveIndexNumber(String email) {
        if (email != null && email.contains("@")) {
            String localPart = email.split("@")[0];
            if (!localPart.isBlank()) {
                return localPart;
            }
        }
        return null;
    }

}
