package com.notus.backend.users.teachercode;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TeacherRegistrationTokenRepository extends JpaRepository<TeacherRegistrationToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TeacherRegistrationToken> findByTokenHash(String tokenHash);
}
