package com.notus.backend.attendance;

import com.notus.backend.attendance.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;
    private final QrTokenService qrTokenService;
    private final QrImageService qrImageService;

    public AttendanceService(
            AttendanceSessionRepository sessionRepo,
            AttendanceRecordRepository recordRepo,
            QrTokenService qrTokenService,
            QrImageService qrImageService
    ) {
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
        this.qrTokenService = qrTokenService;
        this.qrImageService = qrImageService;
    }

    @Transactional
    public com.notus.backend.attendance.dto.CreateSessionResponse createSession(String teacherUid, com.notus.backend.attendance.dto.CreateSessionRequest req) {
        if (req == null || req.title() == null || req.title().isBlank()) {
            throw new IllegalArgumentException("Brak tytułu sesji");
        }

        AttendanceSession s = new AttendanceSession();
        s.setTeacherUid(teacherUid);
        s.setTitle(req.title().trim());
        s.setActive(true);
        s.setCreatedAt(Instant.now());

        s = sessionRepo.save(s);
        return new com.notus.backend.attendance.dto.CreateSessionResponse(s.getId(), s.getTitle(), s.getCreatedAt(), s.isActive());
    }

    @Transactional(readOnly = true)
    public QrResponse generateQr(String teacherUid, Long sessionId) {
        AttendanceSession s = sessionRepo.findByIdAndTeacherUid(sessionId, teacherUid)
                .orElseThrow(() -> new IllegalArgumentException("Sesja nie istnieje lub nie jest Twoja"));

        if (!s.isActive()) {
            throw new IllegalArgumentException("Sesja nieaktywna");
        }

        String qrToken = qrTokenService.createToken(s.getId());
        long expiresAt = Instant.now().getEpochSecond() + qrTokenService.ttlSeconds();

        // QR zawiera token (payload + podpis)
        String pngBase64 = qrImageService.toPngBase64(qrToken, 320);

        return new QrResponse(s.getId(), qrToken, pngBase64, expiresAt);
    }

    @Transactional
    public com.notus.backend.attendance.dto.CheckInResponse checkIn(String studentUid, com.notus.backend.attendance.dto.CheckInRequest req) {
        if (req == null || req.qrToken() == null || req.qrToken().isBlank()) {
            throw new IllegalArgumentException("Brak qrToken");
        }

        var data = qrTokenService.verifyAndParse(req.qrToken());

        AttendanceSession s = sessionRepo.findById(data.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Sesja nie istnieje"));

        if (!s.isActive()) {
            throw new IllegalArgumentException("Sesja nieaktywna");
        }

        // anty-duplikacja
        if (recordRepo.findBySessionIdAndStudentUid(s.getId(), studentUid).isPresent()) {
            throw new IllegalArgumentException("Obecność już zarejestrowana");
        }

        AttendanceRecord r = new AttendanceRecord();
        r.setSessionId(s.getId());
        r.setStudentUid(studentUid);
        r.setCheckedInAt(Instant.now());
        r = recordRepo.save(r);

        return new com.notus.backend.attendance.dto.CheckInResponse(r.getSessionId(), r.getStudentUid(), r.getCheckedInAt());
    }
}
