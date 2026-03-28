package com.notus.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {

    List<Schedule> findByDateBetweenAndTeacherEntityNameContainingIgnoreCase(
            Instant start,
            Instant end,
            String teacherName
    );

    List<Schedule> findByDateBetweenAndTeacherEntityId(
            Instant start,
            Instant end,
            Long teacherId
    );

    List<Schedule> findByDateBetweenAndStudentGroupId(
            Instant start,
            Instant end,
            Long groupId
    );

    List<Schedule> findByStudentGroupIdInOrderByDateAscTimeAsc(List<Long> groupIds);

    List<Schedule> findByDateBetween(Instant start, Instant end);
}