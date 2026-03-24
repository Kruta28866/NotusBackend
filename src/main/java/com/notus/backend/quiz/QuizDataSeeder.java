package com.notus.backend.quiz;

import com.notus.backend.quiz.dto.QuestionDto;
import com.notus.backend.quiz.dto.QuizResponse;
import com.notus.backend.users.Teacher;
import com.notus.backend.users.TeacherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class QuizDataSeeder implements CommandLineRunner {

    private final QuizService quizService;
    private final TeacherRepository teacherRepository;

    public QuizDataSeeder(QuizService quizService, TeacherRepository teacherRepository) {
        this.quizService = quizService;
        this.teacherRepository = teacherRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        String clerkUserId = "user_3BFYEWC78Gr7DaocU0daKeje7XW"; // Clerk ID for "Kamu"

        Optional<Teacher> teacherOpt = teacherRepository.findByClerkUserId(clerkUserId);
        if (teacherOpt.isEmpty()) {
            System.out.println("DEBUG SEEDER: Teacher " + clerkUserId + " not found in DB. Skipping quiz seeding.");
            return;
        }

        Teacher teacher = teacherOpt.get();
        List<Quiz> existing = quizService.getTeacherQuizzes(clerkUserId);
        System.out.println("DEBUG SEEDER: Quizzes for " + clerkUserId + " found: " + existing.size());
        if (existing.isEmpty()) {
            System.out.println("Seeding quiz data for teacher Kamu...");

            QuizResponse quiz = new QuizResponse();
            quiz.setTitle("Kolokwium: Bezpieczeństwo Sieci");
            quiz.setDescription("Podstawowe zagadnienia z zakresu bezpieczeństwa systemów i sieci komputerowych.");

            List<QuestionDto> questions = new ArrayList<>();

            // Question 1 (CLOSED)
            QuestionDto q1 = new QuestionDto();
            q1.setQuestion("Co oznacza skrót CIA w kontekście bezpieczeństwa?");
            q1.setType(QuestionType.CLOSED);
            q1.setOptions(List.of(
                "Confidentiality, Integrity, Availability",
                "Central Intelligence Agency",
                "Control, Information, Access",
                "Cryptography, Identity, Authentication"
            ));
            q1.setCorrectAnswer("Confidentiality, Integrity, Availability");
            questions.add(q1);

            // Question 2 (CLOSED)
            QuestionDto q2 = new QuestionDto();
            q2.setQuestion("Który protokół służy do bezpiecznego przesyłania plików?");
            q2.setType(QuestionType.CLOSED);
            q2.setOptions(List.of("FTP", "HTTP", "SFTP", "Telnet"));
            q2.setCorrectAnswer("SFTP");
            questions.add(q2);

            // Question 3 (OPEN)
            QuestionDto q3 = new QuestionDto();
            q3.setQuestion("Opisz krótko różnicę między szyfrowaniem symetrycznym a asymetrycznym.");
            q3.setType(QuestionType.OPEN);
            questions.add(q3);

            quiz.setQuestions(questions);

            quizService.saveQuiz(clerkUserId, quiz);
            System.out.println("Quiz data seeded successfully!");
        }
    }
}
