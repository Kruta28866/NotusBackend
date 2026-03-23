package com.notus.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {
    List<Schedule> findByDateBetweenAndTeacherContainingIgnoreCase(Instant start, Instant end, String teacher);
    List<Schedule> findByDateBetween(Instant start, Instant end);
}
