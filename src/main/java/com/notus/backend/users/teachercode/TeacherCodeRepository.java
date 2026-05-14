package com.notus.backend.users.teachercode;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherCodeRepository extends JpaRepository<TeacherCode, Long> {

    Optional<TeacherCode> findByCode(String code);

    Optional<TeacherCode> findByCodeHash(String codeHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select teacherCode from TeacherCode teacherCode where teacherCode.code = :code")
    Optional<TeacherCode> findByCodeForUpdate(@Param("code") String code);

    List<TeacherCode> findByIsActiveTrueOrderByCreatedAtDesc();

    boolean existsByCode(String code);

    boolean existsByCodeHash(String codeHash);
}
