package com.notus.backend.history;

import com.notus.backend.history.dto.SessionStudentResultDto;
import com.notus.backend.history.dto.StudentHistoryItemDto;
import com.notus.backend.history.dto.TeacherHistoryItemDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;
    private final HistoryPdfService historyPdfService;

    public HistoryController(HistoryService historyService, HistoryPdfService historyPdfService) {
        this.historyService = historyService;
        this.historyPdfService = historyPdfService;
    }

    @GetMapping("/teacher")
    public List<TeacherHistoryItemDto> getTeacherHistory(Authentication auth) {
        String uid = (String) auth.getPrincipal();
        return historyService.getTeacherHistory(uid);
    }

    @GetMapping("/student")
    public List<StudentHistoryItemDto> getStudentHistory(Authentication auth) {
        String uid = (String) auth.getPrincipal();
        return historyService.getStudentHistory(uid);
    }
    @GetMapping("/teacher/session/{scheduleId}")
    public List<SessionStudentResultDto> getSessionDetails(@PathVariable String scheduleId) {
        return historyService.getSessionDetails(scheduleId);
    }

    @GetMapping(value = "/teacher/session/{scheduleId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadSessionPdf(@PathVariable String scheduleId) throws IOException {
        List<SessionStudentResultDto> results = historyService.getSessionDetails(scheduleId);
        byte[] pdfBytes = historyPdfService.generateSessionSummaryPdf(scheduleId, results);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"podsumowanie-" + scheduleId + ".pdf\"")
                .body(pdfBytes);
    }
}
