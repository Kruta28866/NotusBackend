package com.notus.backend.history;

import com.notus.backend.history.dto.SessionStudentResultDto;
import com.notus.backend.schedule.Schedule;
import com.notus.backend.schedule.ScheduleRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HistoryPdfService {

    private final ScheduleRepository scheduleRepository;

    public HistoryPdfService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public byte[] generateSessionSummaryPdf(String scheduleId, List<SessionStudentResultDto> results) throws IOException {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        String subjectName = schedule != null ? schedule.getSubject() : "Zajecia";
        String dateStr = schedule != null && schedule.getDate() != null 
                ? schedule.getDate().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) 
                : "";

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float margin = 50;
            float yStart = PDRectangle.A4.getHeight() - margin;
            float y = yStart;

            // Title
            cs.beginText();
            cs.setFont(fontBold, 18);
            cs.newLineAtOffset(margin, y);
            cs.showText(sanitize("Podsumowanie Sesji: " + subjectName));
            cs.endText();
            y -= 25;

            // Date & Time
            if (schedule != null) {
                cs.beginText();
                cs.setFont(fontRegular, 12);
                cs.newLineAtOffset(margin, y);
                String timeSpan = schedule.getTime() != null ? " (" + schedule.getTime() + ")" : "";
                cs.showText("Data: " + dateStr + timeSpan);
                cs.endText();
                y -= 30;
            }

            // Table headers
            cs.setFont(fontBold, 12);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("Student");
            cs.newLineAtOffset(200, 0);
            cs.showText("Obecnosc");
            cs.newLineAtOffset(150, 0);
            cs.showText("Wynik Quizu");
            cs.endText();
            y -= 5;
            
            cs.moveTo(margin, y);
            cs.lineTo(PDRectangle.A4.getWidth() - margin, y);
            cs.stroke();
            y -= 20;

            // Write results row by row
            cs.setFont(fontRegular, 11);
            for (SessionStudentResultDto res : results) {
                if (y < margin) { // paginate
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    cs.setFont(fontRegular, 11);
                    y = yStart;
                }

                String name = sanitize(res.getStudentName());
                String att = res.isAttended() ? "Obecny" : "Nieobecny";
                String quizStr = "Brak";
                if (res.getQuizTotal() != null && res.getQuizTotal() > 0) {
                    int score = res.getQuizScore() != null ? res.getQuizScore() : 0;
                    int pct = Math.round(((float)score / res.getQuizTotal()) * 100);
                    quizStr = score + "/" + res.getQuizTotal() + " (" + pct + "%)";
                }

                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(name);
                cs.newLineAtOffset(200, 0);
                cs.showText(att);
                cs.newLineAtOffset(150, 0);
                cs.showText(quizStr);
                cs.endText();

                y -= 20;
            }

            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\x00-\\x1F\\x7F]", "")
                   .replaceAll("[^\\x00-\\x7F]", "?"); // using base PDF ASCII friendly regex
    }
}
