package com.notus.backend.quiz;

import com.notus.backend.users.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    List<QuizSubmission> findByAssignment(QuizAssignment assignment);

    Optional<QuizSubmission> findByAssignmentAndStudent(QuizAssignment assignment, Student student);

    List<QuizSubmission> findByStudent(Student student);

    boolean existsByAssignmentAndStudent(QuizAssignment assignment, Student student);
    boolean existsByAssignment(QuizAssignment assignment);

    List<QuizSubmission> findByStudentAndReviewedAtIsNotNullAndNotificationSeenFalse(Student student);
}
