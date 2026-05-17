package com.notus.backend.teachergroups;

import com.notus.backend.users.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    long countByGroupAndStatus(TeacherGroup group, GroupMembershipStatus status);
    List<GroupMembership> findByGroupAndStatusOrderByJoinedAtAsc(TeacherGroup group, GroupMembershipStatus status);
    Optional<GroupMembership> findByGroupAndStudent(TeacherGroup group, Student student);
    Optional<GroupMembership> findByGroupAndStudentAndStatus(TeacherGroup group, Student student, GroupMembershipStatus status);
    Optional<GroupMembership> findByGroupIdAndStudentIdAndStatus(Long groupId, Long studentId, GroupMembershipStatus status);
    boolean existsByGroupAndStudentAndStatus(TeacherGroup group, Student student, GroupMembershipStatus status);
    List<GroupMembership> findByStudentAndStatusOrderByJoinedAtDesc(Student student, GroupMembershipStatus status);

    @Query("select m from GroupMembership m where m.group.teacher = :teacher order by m.joinedAt desc")
    List<GroupMembership> findByTeacherOrderByJoinedAtDesc(com.notus.backend.users.Teacher teacher);
}
