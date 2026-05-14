package com.notus.backend.teachergroups;

import com.notus.backend.users.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    long countByGroupAndStatus(TeacherGroup group, GroupMembershipStatus status);
    List<GroupMembership> findByGroupAndStatusOrderByJoinedAtAsc(TeacherGroup group, GroupMembershipStatus status);
    Optional<GroupMembership> findByGroupAndStudent(TeacherGroup group, Student student);
    Optional<GroupMembership> findByGroupAndStudentAndStatus(TeacherGroup group, Student student, GroupMembershipStatus status);
    Optional<GroupMembership> findByGroupIdAndStudentIdAndStatus(Long groupId, Long studentId, GroupMembershipStatus status);
}
