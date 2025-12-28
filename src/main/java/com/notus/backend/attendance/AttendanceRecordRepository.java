package com.notus.backend.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findBySessionIdAndStudentUid(Long sessionId, String studentUid);
}
