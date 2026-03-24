package com.notus.backend.attendance;

import com.notus.backend.users.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findBySessionIdAndStudent(Long sessionId, Student student);

    List<AttendanceRecord> findBySessionId(Long sessionId);
}
