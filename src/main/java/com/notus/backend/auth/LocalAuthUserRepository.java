package com.notus.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalAuthUserRepository extends JpaRepository<LocalAuthUser, Long> {
    Optional<LocalAuthUser> findByEmailIgnoreCase(String email);
    Optional<LocalAuthUser> findByAuthUserId(String authUserId);
    Optional<LocalAuthUser> findByEmailVerificationTokenHash(String tokenHash);
    boolean existsByEmailIgnoreCase(String email);
}
