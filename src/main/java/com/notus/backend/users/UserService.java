package com.notus.backend.users;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final com.notus.backend.attendance.group.StudentGroupRepository studentGroupRepo;

    public UserService(StudentRepository studentRepo, TeacherRepository teacherRepo, com.notus.backend.attendance.group.StudentGroupRepository studentGroupRepo) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.studentGroupRepo = studentGroupRepo;
    }

    @Transactional
    public UserDto findOrCreate(String clerkUserId, String email, String name) {
        Role targetRole = roleFromEmail(email);

        Optional<Teacher> existingTeacher = teacherRepo.findByClerkUserId(clerkUserId);
        if (existingTeacher.isPresent()) {
            Teacher teacher = existingTeacher.get();

            if (targetRole == Role.STUDENT) {
                teacherRepo.delete(teacher);
                Student student = createStudent(clerkUserId, email, name);
                return mapStudentToDto(student);
            }

            updateTeacherData(teacher, email, name);
            teacher = teacherRepo.save(teacher);
            return mapTeacherToDto(teacher);
        }

        Optional<Student> existingStudent = studentRepo.findByClerkUserId(clerkUserId);
        if (existingStudent.isPresent()) {
            Student student = existingStudent.get();

            if (targetRole == Role.TEACHER) {
                studentRepo.delete(student);
                Teacher teacher = createTeacher(clerkUserId, email, name);
                return mapTeacherToDto(teacher);
            }

            updateStudentData(student, email, name);
            student = studentRepo.save(student);
            return mapStudentToDto(student);
        }

        if (targetRole == Role.STUDENT) {
            Student student = createStudent(clerkUserId, email, name);
            return mapStudentToDto(student);
        } else {
            Teacher teacher = createTeacher(clerkUserId, email, name);
            return mapTeacherToDto(teacher);
        }
    }

    public Optional<Student> findStudentByUid(String uid) {
        return studentRepo.findByClerkUserId(uid);
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

    private Role roleFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return Role.STUDENT;
        }

        String e = email.trim().toLowerCase();

        if (e.startsWith("s")) {
            return Role.STUDENT;
        }

        if (e.endsWith("@gmail.com")) {
            return Role.TEACHER;
        }

        if (e.endsWith("@pjwstk.edu.pl") && !e.startsWith("s")) {
            return Role.TEACHER;
        }

        return Role.TEACHER;
    }
}