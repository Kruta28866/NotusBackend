package com.notus.backend.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByIdAndTeacherUid(Long id, String teacherUid);
}

