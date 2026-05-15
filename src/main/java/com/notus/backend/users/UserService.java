package com.notus.backend.users;

import com.notus.backend.users.teachercode.TeacherCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final com.notus.backend.attendance.group.StudentGroupRepository studentGroupRepo;
    private final TeacherCodeService teacherCodeService;

    public UserService(StudentRepository studentRepo,
                       TeacherRepository teacherRepo,
                       com.notus.backend.attendance.group.StudentGroupRepository studentGroupRepo,
                       TeacherCodeService teacherCodeService) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.studentGroupRepo = studentGroupRepo;
        this.teacherCodeService = teacherCodeService;
    }

    @Transactional
    public UserDto findOrCreate(String clerkUserId, String email, String name) {
        Optional<Teacher> existingTeacher = teacherRepo.findByClerkUserId(clerkUserId);
        if (existingTeacher.isPresent()) {
            Teacher teacher = existingTeacher.get();
            updateTeacherData(teacher, email, name);
            teacher = teacherRepo.save(teacher);
            return mapTeacherToDto(teacher);
        }

        Optional<Student> existingStudent = studentRepo.findByClerkUserId(clerkUserId);
        if (existingStudent.isPresent()) {
            Student student = existingStudent.get();
            updateStudentData(student, email, name);
            student = studentRepo.save(student);
            return mapStudentToDto(student);
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Wybierz typ konta przed rejestracją");
    }

    @Transactional
    public UserDto findOrCreate(String clerkUserId, String email, String name, Role requestedRole, String providedTeacherAccessCode) {
        Optional<UserDto> existingUser = findExistingByUid(clerkUserId);
        if (existingUser.isPresent()) {
            UserDto user = existingUser.get();

            if (requestedRole != null && user.role() != requestedRole) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "To konto jest już zarejestrowane jako " + user.role());
            }

            if (user.role() == Role.TEACHER && requestedRole == Role.TEACHER && hasText(providedTeacherAccessCode)) {
                teacherCodeService.validateCode(providedTeacherAccessCode);
            }

            return findOrCreate(clerkUserId, email, name);
        }

        if (requestedRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Typ konta jest wymagany");
        }

        if (requestedRole == Role.TEACHER) {
            teacherCodeService.consumeCode(providedTeacherAccessCode);
            Teacher teacher = createTeacher(clerkUserId, email, name);
            return mapTeacherToDto(teacher);
        }

        if (requestedRole == Role.STUDENT) {
            Student student = createStudent(clerkUserId, email, name);
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
            updateStudentData(student, email, name);
            return studentRepo.save(student);
        }

        Optional<Student> existingByEmail = studentRepo.findByEmailIgnoreCase(email);
        if (existingByEmail.isPresent()) {
            Student student = existingByEmail.get();
            student.setClerkUserId(clerkUserId);
            updateStudentData(student, email, name);
            return studentRepo.save(student);
        }

        return createStudent(clerkUserId, email, name);
    }

    public Optional<Student> findStudentWithGroupsByUid(String uid) {
        return studentRepo.findWithStudentGroupsByClerkUserId(uid);
    }

    public Optional<Teacher> findTeacherByUid(String uid) {
        return teacherRepo.findByClerkUserId(uid);
    }

    private Student createStudent(String clerkUserId, String email, String name) {
        Student student = new Student();
        student.setClerkUserId(clerkUserId);
        student.setEmail(resolveEmail(clerkUserId, email));
        student.setName(resolveName(email, name));
        student.setRole(Role.STUDENT);
        student.setIndexNumber(resolveIndexNumber(email));
        
        // Auto-assign to default group if exists
        studentGroupRepo.findByCode("INF-2024-SEM2").ifPresent(group -> {
            student.setStudentGroups(java.util.List.of(group));
        });
        
        return studentRepo.save(student);
    }

    private Teacher createTeacher(String clerkUserId, String email, String name) {
        Teacher teacher = new Teacher();
        teacher.setClerkUserId(clerkUserId);
        teacher.setEmail(resolveEmail(clerkUserId, email));
        teacher.setName(resolveName(email, name));
        teacher.setRole(Role.TEACHER);
        return teacherRepo.save(teacher);
    }

    private void updateStudentData(Student student, String email, String name) {
        if (email != null && !email.isBlank()) {
            student.setEmail(email);
        }

        student.setName(resolveName(student.getEmail(), name));
        student.setIndexNumber(resolveIndexNumber(student.getEmail()));
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
                student.getIndexNumber()
        );
    }

    private UserDto mapTeacherToDto(Teacher teacher) {
        return new UserDto(
                teacher.getId(),
                teacher.getEmail(),
                teacher.getName(),
                teacher.getRole(),
                null
        );
    }

    private String resolveEmail(String clerkUserId, String email) {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return clerkUserId + "@temporary.com";
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
