package com.notus.backend.users;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByClerkUserId(String clerkUserId);
    Optional<Student> findByEmail(String email);
}
