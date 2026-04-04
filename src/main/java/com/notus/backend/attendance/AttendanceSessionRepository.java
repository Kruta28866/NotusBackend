package com.notus.backend.attendance;

import com.notus.backend.users.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByTeacher(Teacher teacher);
    Optional<AttendanceSession> findByIdAndTeacher(Long id, Teacher teacher);
    Optional<AttendanceSession> findByShortCode(String shortCode);
    Optional<AttendanceSession> findByScheduleIdAndActiveTrue(String scheduleId);
    Optional<AttendanceSession> findFirstByScheduleIdOrderByCreatedAtDesc(String scheduleId);
}