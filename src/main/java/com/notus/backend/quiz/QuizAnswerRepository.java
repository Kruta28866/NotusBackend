package com.notus.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    List<QuizAnswer> findBySubmission(QuizSubmission submission);
}
