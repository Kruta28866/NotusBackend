package com.notus.backend.users;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByClerkUserId(String clerkUserId);

    @EntityGraph(attributePaths = "studentGroups")
    Optional<Student> findWithStudentGroupsByClerkUserId(String clerkUserId);

    Optional<Student> findByEmail(String email);
    Optional<Student> findByEmailIgnoreCase(String email);

    List<Student> findByStudentGroupsId(Long groupId);

    List<Student> findByRoleAndEmailContainingIgnoreCaseOrderByEmailAsc(Role role, String email, Pageable pageable);
}
