package com.notus.backend.quiz;

import com.notus.backend.users.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByTeacher(Teacher teacher);
    List<Quiz> findByTeacherAndArchivedFalse(Teacher teacher);
}
