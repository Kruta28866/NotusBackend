package com.notus.backend.quiz;

import com.notus.backend.quiz.dto.QuizResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    private final PdfQuizService pdfQuizService;
    private final QuizService quizService;

    public QuizController(PdfQuizService pdfQuizService, QuizService quizService) {
        this.pdfQuizService = pdfQuizService;
        this.quizService = quizService;
    }

    @PostMapping(value = "/from-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuizResponse generateQuizFromPdf(@RequestParam("file") MultipartFile file) {
        return pdfQuizService.generateQuizFromPdf(file);
    }

    @PostMapping("/save")
    public Quiz saveQuiz(Principal principal, @RequestBody QuizResponse quizResponse) {
        String uid = principal.getName();
        return quizService.saveQuiz(uid, quizResponse);
    }

    @GetMapping("/my")
    public List<Quiz> getMyQuizzes(Principal principal) {
        String uid = principal.getName();
        List<Quiz> quizzes = quizService.getTeacherQuizzes(uid);
        System.out.println("DEBUG: Fetching quizzes for UID: [" + uid + "], result size: " + (quizzes != null ? quizzes.size() : "null"));
        return quizzes;
    }

    @GetMapping("/{id}")
    public Quiz getQuizDetails(Principal principal, @PathVariable Long id) {
        String uid = principal.getName();
        return quizService.getQuizDetails(uid, id);
    }

    @DeleteMapping("/{id}")
    public void deleteQuiz(Principal principal, @PathVariable Long id) {
        String uid = principal.getName();
        quizService.deleteQuiz(uid, id);
    }
}