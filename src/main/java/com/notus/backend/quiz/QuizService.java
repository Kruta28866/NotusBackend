package com.notus.backend.quiz;

import com.notus.backend.quiz.dto.QuestionDto;
import com.notus.backend.quiz.dto.QuizResponse;
import com.notus.backend.users.Teacher;
import com.notus.backend.users.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final TeacherRepository teacherRepository;

    public QuizService(QuizRepository quizRepository, TeacherRepository teacherRepository) {
        this.quizRepository = quizRepository;
        this.teacherRepository = teacherRepository;
    }

    private Teacher getTeacherByClerkId(String clerkUserId) {
        return teacherRepository.findByClerkUserId(clerkUserId)
                .orElseThrow(() -> new IllegalArgumentException("Nauczyciel nie istnieje: " + clerkUserId));
    }

    @Transactional
    public Quiz saveQuiz(String clerkUserId, QuizResponse dto) {
        Teacher teacher = getTeacherByClerkId(clerkUserId);
        Quiz quiz = new Quiz();
        quiz.setTeacher(teacher);
        quiz.setTitle(dto.getTitle());
        quiz.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        quiz.setCreatedAt(Instant.now());

        if (dto.getQuestions() != null) {
            for (QuestionDto qDto : dto.getQuestions()) {
                QuizQuestion question = new QuizQuestion();
                question.setQuestionText(qDto.getQuestion());
                question.setType(qDto.getType() != null ? qDto.getType() : QuestionType.CLOSED);
                question.setOptions(qDto.getOptions());
                question.setCorrectAnswer(qDto.getCorrectAnswer());
                quiz.addQuestion(question);
            }
        }

        return quizRepository.save(quiz);
    }

    public List<Quiz> getTeacherQuizzes(String clerkUserId) {
        Teacher teacher = getTeacherByClerkId(clerkUserId);
        return quizRepository.findByTeacher(teacher);
    }

    public Quiz getQuizDetails(String clerkUserId, Long quizId) {
        Teacher teacher = getTeacherByClerkId(clerkUserId);
         Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz nie istnieje"));
        
        if (!quiz.getTeacher().equals(teacher)) {
            throw new IllegalArgumentException("Brak uprawnień do tego quizu");
        }
        
        return quiz;
    }

    @Transactional
    public void deleteQuiz(String clerkUserId, Long quizId) {
        Quiz quiz = getQuizDetails(clerkUserId, quizId);
        quizRepository.delete(quiz);
    }
}
