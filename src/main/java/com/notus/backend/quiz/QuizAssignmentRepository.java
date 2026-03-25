package com.notus.backend.quiz;

import com.notus.backend.users.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAssignmentRepository extends JpaRepository<QuizAssignment, Long> {

    List<QuizAssignment> findByTeacher(Teacher teacher);

    List<QuizAssignment> findByScheduleIdIn(List<String> scheduleIds);

    Optional<QuizAssignment> findByScheduleId(String scheduleId);

    boolean existsByQuizIdAndScheduleId(Long quizId, String scheduleId);

    Optional<QuizAssignment> findBySessionIdAndActiveTrue(Long sessionId);
}
