package com.notus.backend.users;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;

    public UserService(StudentRepository studentRepo, TeacherRepository teacherRepo) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
    }

    @Transactional
    public UserDto findOrCreate(String clerkUserId, String email, String name) {
        Role targetRole = roleFromEmail(email);

        // 1. Try to find existing first by UID
        Optional<Teacher> tOpt = teacherRepo.findByClerkUserId(clerkUserId);
        if (tOpt.isPresent()) {
            Teacher t = tOpt.get();

            // Check if we should move them to Students (rare but possible)
            if (targetRole == Role.STUDENT) {
                studentRepo.findByClerkUserId(clerkUserId).ifPresentOrElse(
                    s -> {}, // Already spans both? Should not happen.
                    () -> {
                        Student s = new Student();
                        s.setClerkUserId(clerkUserId);
                        s.setEmail(t.getEmail());
                        s.setName(t.getName());
                        s.setRole(Role.STUDENT);
                        studentRepo.save(s);
                    }
                );
                teacherRepo.delete(t);
                return findOrCreate(clerkUserId, email, name); // retry
            }

            if (email != null && !email.isBlank()) t.setEmail(email);
            if (name != null && !name.isBlank()) {
                t.setName(name);
            } else if (("User".equals(t.getName()) || t.getName() == null) && email != null && email.contains("@")) {
                t.setName(email.split("@")[0]);
            }
            teacherRepo.save(t);
            return new UserDto(t.getId(), t.getEmail(), t.getName(), t.getRole(), null);
        }

        Optional<Student> sOpt = studentRepo.findByClerkUserId(clerkUserId);
        if (sOpt.isPresent()) {
            Student s = sOpt.get();

            // Check if we should promote them to Teacher
            if (targetRole == Role.TEACHER) {
                teacherRepo.findByClerkUserId(clerkUserId).ifPresentOrElse(
                    t -> {},
                    () -> {
                        Teacher t = new Teacher();
                        t.setClerkUserId(clerkUserId);
                        t.setEmail(s.getEmail());
                        t.setName(s.getName());
                        t.setRole(Role.TEACHER);
                        teacherRepo.save(t);
                    }
                );
                studentRepo.delete(s);
                return findOrCreate(clerkUserId, email, name); // retry
            }

            if (email != null && !email.isBlank()) s.setEmail(email);

            if (name != null && !name.isBlank()) {
                s.setName(name);
            } else if (("User".equals(s.getName()) || s.getName() == null) && email != null && email.contains("@")) {
                s.setName(email.split("@")[0]);
            }

            studentRepo.save(s);
            return new UserDto(s.getId(), s.getEmail(), s.getName(), s.getRole(), s.getIndexNumber());
        }

        // 2. If new user, create appropriate record
        String derivedName = (name != null && !name.isBlank()) ? name : "User";
        if ("User".equals(derivedName) && email != null && email.contains("@")) {
            derivedName = email.split("@")[0];
        }

        if (targetRole == Role.STUDENT) {
            Student s = new Student();
            s.setClerkUserId(clerkUserId);
            s.setEmail(email != null ? email : clerkUserId + "@temporary.com");
            s.setName(derivedName);
            s.setRole(Role.STUDENT);

            if (email != null && email.contains("@")) {
                String local = email.split("@")[0];
                if (!local.isBlank()) s.setIndexNumber(local);
            }
            s = studentRepo.save(s);
            return new UserDto(s.getId(), s.getEmail(), s.getName(), s.getRole(), s.getIndexNumber());
        } else {
            Teacher t = new Teacher();
            t.setClerkUserId(clerkUserId);
            t.setEmail(email != null ? email : clerkUserId + "@temporary.com");
            t.setName(derivedName);
            t.setRole(targetRole);
            t = teacherRepo.save(t);
            return new UserDto(t.getId(), t.getEmail(), t.getName(), t.getRole(), null);
        }
    }

    public Optional<Student> findStudentByUid(String uid) {
        return studentRepo.findByClerkUserId(uid);
    }

    public Optional<Teacher> findTeacherByUid(String uid) {
        return teacherRepo.findByClerkUserId(uid);
    }

    private Role roleFromEmail(String email) {
        if (email == null) return Role.STUDENT;

        String e = email.trim().toLowerCase();
        // convention: students start with 's' followed by index number
        if (e.startsWith("s")) return Role.STUDENT;
        
        // backup for specific domains if needed, but primary is 's' prefix
        if (e.endsWith("@gmail.com")) return Role.TEACHER;
        if (e.endsWith("@pjwstk.edu.pl") && !e.startsWith("s")) return Role.TEACHER;

        return Role.TEACHER;
    }
}
