package com.notus.backend.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByAuthUserId(String authUserId);

    Optional<AppUser> findByEmailIgnoreCase(String email);
}
