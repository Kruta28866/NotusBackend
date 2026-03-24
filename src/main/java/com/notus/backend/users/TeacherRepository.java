package com.notus.backend.users;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByClerkUserId(String clerkUserId);
    Optional<Teacher> findByEmail(String email);
}
