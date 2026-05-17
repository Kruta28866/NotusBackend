package com.notus.backend.teachergroups;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    Optional<GroupInvitation> findByTokenHash(String tokenHash);
    Optional<GroupInvitation> findByIdAndGroup(Long id, TeacherGroup group);
    List<GroupInvitation> findByGroupOrderByCreatedAtDesc(TeacherGroup group);
    List<GroupInvitation> findByGroupAndEmailIgnoreCaseOrderByCreatedAtDesc(TeacherGroup group, String email);
    List<GroupInvitation> findByCreatedByTeacherOrderByCreatedAtDesc(com.notus.backend.users.Teacher teacher);
}
