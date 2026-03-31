package com.notus.backend.attendance;

import com.notus.backend.users.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByIdAndTeacher(Long id, Teacher teacher);
    Optional<AttendanceSession> findByShortCode(String shortCode);
    Optional<AttendanceSession> findByScheduleIdAndActiveTrue(String scheduleId);
}