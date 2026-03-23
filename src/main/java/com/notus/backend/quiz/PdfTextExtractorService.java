package com.notus.backend.quiz;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfTextExtractorService {

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Plik PDF jest pusty.");
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return cleanText(text);
        } catch (IOException e) {
            throw new RuntimeException("Nie udało się odczytać pliku PDF.", e);
        }
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}