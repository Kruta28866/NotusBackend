package com.notus.backend.attendance;

import com.notus.backend.attendance.dto.*;
import com.notus.backend.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;
    private final QrTokenService qrTokenService;
    private final QrImageService qrImageService;
    private final UserRepository userRepo;

    public AttendanceService(
            AttendanceSessionRepository sessionRepo,
            AttendanceRecordRepository recordRepo,
            QrTokenService qrTokenService,
            QrImageService qrImageService,
            UserRepository userRepo
    ) {
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
        this.qrTokenService = qrTokenService;
        this.qrImageService = qrImageService;
        this.userRepo = userRepo;
    }

    @Transactional
    public CreateSessionResponse createSession(String teacherUid, CreateSessionRequest req) {
        if (req == null || req.title() == null || req.title().isBlank()) {
            throw new IllegalArgumentException("Brak tytułu sesji");
        }

        AttendanceSession s = new AttendanceSession();
        s.setTeacherUid(teacherUid);
        s.setTitle(req.title().trim());
        s.setActive(true);
        s.setCreatedAt(Instant.now());

        s = sessionRepo.save(s);
        return new CreateSessionResponse(s.getId(), s.getTitle(), s.getCreatedAt(), s.isActive());
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
        String pngBase64 = qrImageService.toPngBase64(qrToken, 320);

        return new QrResponse(s.getId(), qrToken, pngBase64, expiresAt);
    }

    @Transactional
    public CheckInResponse checkIn(String studentUid, CheckInRequest req) {
        if (req == null || req.qrToken() == null || req.qrToken().isBlank()) {
            throw new IllegalArgumentException("Brak qrToken");
        }

        var data = qrTokenService.verifyAndParse(req.qrToken());

        AttendanceSession s = sessionRepo.findById(data.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Sesja nie istnieje"));

        if (!s.isActive()) {
            throw new IllegalArgumentException("Sesja nieaktywna");
        }

        if (recordRepo.findBySessionIdAndStudentUid(s.getId(), studentUid).isPresent()) {
            throw new IllegalArgumentException("Obecność już zarejestrowana");
        }

        AttendanceRecord r = new AttendanceRecord();
        r.setSessionId(s.getId());
        r.setStudentUid(studentUid);
        r.setCheckedInAt(Instant.now());
        r = recordRepo.save(r);

        var user = userRepo.findByClerkUserId(studentUid).orElse(null);
        String name = user != null ? user.getName() : studentUid;
        String index = user != null ? user.getIndexNumber() : null;

        return new CheckInResponse(r.getSessionId(), r.getStudentUid(), name, index, r.getCheckedInAt());
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> getRecordsForSession(String teacherUid, Long sessionId) {
        sessionRepo.findByIdAndTeacherUid(sessionId, teacherUid)
                .orElseThrow(() -> new IllegalArgumentException("Sesja nie istnieje lub nie jest Twoja"));

        return recordRepo.findBySessionId(sessionId)
                .stream()
                .map(r -> {
                    var user = userRepo.findByClerkUserId(r.getStudentUid()).orElse(null);
                    String name = user != null ? user.getName() : r.getStudentUid();
                    String index = user != null ? user.getIndexNumber() : null;
                    return new CheckInResponse(
                            r.getSessionId(),
                            r.getStudentUid(),
                            name,
                            index,
                            r.getCheckedInAt()
                    );
                })
                .toList();
    }
}
