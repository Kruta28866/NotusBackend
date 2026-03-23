package com.notus.backend.quiz;

import com.notus.backend.quiz.dto.QuizResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final PdfQuizService pdfQuizService;

    public QuizController(PdfQuizService pdfQuizService) {
        this.pdfQuizService = pdfQuizService;
    }

    @PostMapping(value = "/from-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuizResponse generateQuizFromPdf(@RequestParam("file") MultipartFile file) {
        return pdfQuizService.generateQuizFromPdf(file);
    }
}