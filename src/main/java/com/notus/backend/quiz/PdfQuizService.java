package com.notus.backend.quiz;

import com.notus.backend.quiz.dto.QuizResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfQuizService {

    private final PdfTextExtractorService pdfTextExtractorService;
    private final GeminiQuizService geminiQuizService;

    public PdfQuizService(PdfTextExtractorService pdfTextExtractorService,
                          GeminiQuizService geminiQuizService) {
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.geminiQuizService = geminiQuizService;
    }

    public QuizResponse generateQuizFromPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Nie wybrano pliku.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new RuntimeException("Dozwolony jest tylko plik PDF.");
        }

        String text = pdfTextExtractorService.extractText(file);

        if (text.isBlank()) {
            throw new RuntimeException("Nie udało się wyciągnąć tekstu z PDF.");
        }

        return geminiQuizService.generateQuiz(text);
    }
}