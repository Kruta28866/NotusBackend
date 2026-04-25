package com.notus.backend.attendance.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    Optional<StudentGroup> findByCode(String code);
}
