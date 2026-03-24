package com.notus.backend.attendance;

import com.notus.backend.attendance.dto.*;
import com.notus.backend.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak tytułu sesji");
        }

        AttendanceSession s = new AttendanceSession();
        s.setTeacherUid(teacherUid);
        s.setTitle(req.title().trim());
        s.setShortCode(generateShortCode());
        s.setActive(true);
        s.setCreatedAt(Instant.now());

        s = sessionRepo.save(s);
        return new CreateSessionResponse(
                s.getId(),
                s.getTitle(),
                s.getCreatedAt(),
                s.isActive()
        );
    }

    @Transactional(readOnly = true)
    public QrResponse generateQr(String teacherUid, Long sessionId) {
        AttendanceSession s = sessionRepo.findByIdAndTeacherUid(sessionId, teacherUid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sesja nie istnieje lub nie jest Twoja"
                ));

        if (!s.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sesja nieaktywna");
        }

        String qrToken = qrTokenService.createToken(s.getId());
        long expiresAt = Instant.now().getEpochSecond() + qrTokenService.ttlSeconds();
        String pngBase64 = qrImageService.toPngBase64(qrToken, 320);

        return new QrResponse(
                s.getId(),
                qrToken,
                pngBase64,
                expiresAt,
                s.getShortCode()
        );
    }

    @Transactional
    public CheckInResponse checkIn(String studentUid, CheckInRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak danych check-in");
        }

        AttendanceSession s;

        if (req.qrToken() != null && !req.qrToken().isBlank()) {
            var data = qrTokenService.verifyAndParse(req.qrToken());

            s = sessionRepo.findById(data.sessionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Sesja nie istnieje"
                    ));
        } else if (req.shortCode() != null && !req.shortCode().isBlank()) {
            s = sessionRepo.findByShortCode(req.shortCode().trim().toUpperCase())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Kod jest niepoprawny."
                    ));
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Brak qrToken lub shortCode"
            );
        }

        if (!s.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sesja nieaktywna");
        }

        var user = userRepo.findByClerkUserId(studentUid).orElse(null);
        String name = user != null ? user.getName() : studentUid;
        String index = user != null ? user.getIndexNumber() : null;

        var existing = recordRepo.findBySessionIdAndStudentUid(s.getId(), studentUid);
        if (existing.isPresent()) {
            AttendanceRecord existingRecord = existing.get();

            return new CheckInResponse(
                    existingRecord.getSessionId(),
                    s.getTitle(),
                    existingRecord.getStudentUid(),
                    name,
                    index,
                    existingRecord.getCheckedInAt(),
                    true,
                    s.getEndsAt()
            );
        }

        AttendanceRecord r = new AttendanceRecord();
        r.setSessionId(s.getId());
        r.setStudentUid(studentUid);
        r.setCheckedInAt(Instant.now());
        r = recordRepo.save(r);

        return new CheckInResponse(
                r.getSessionId(),
                s.getTitle(),
                r.getStudentUid(),
                name,
                index,
                r.getCheckedInAt(),
                false,
                s.getEndsAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> getRecordsForSession(String teacherUid, Long sessionId) {
        AttendanceSession session = sessionRepo.findByIdAndTeacherUid(sessionId, teacherUid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sesja nie istnieje lub nie jest Twoja"
                ));

        return recordRepo.findBySessionId(sessionId)
                .stream()
                .map(r -> {
                    var user = userRepo.findByClerkUserId(r.getStudentUid()).orElse(null);
                    String name = user != null ? user.getName() : r.getStudentUid();
                    String index = user != null ? user.getIndexNumber() : null;

                    return new CheckInResponse(
                            r.getSessionId(),
                            session.getTitle(),
                            r.getStudentUid(),
                            name,
                            index,
                            r.getCheckedInAt(),
                            false,
                            session.getEndsAt()
                    );
                })
                .toList();
    }

    private String generateShortCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }

        return code.toString();
    }
}