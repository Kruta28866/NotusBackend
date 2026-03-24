package com.notus.backend.attendance;

import com.notus.backend.attendance.dto.*;
import com.notus.backend.users.Student;
import com.notus.backend.users.StudentRepository;
import com.notus.backend.users.TeacherRepository;
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
    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;

    public AttendanceService(
            AttendanceSessionRepository sessionRepo,
            AttendanceRecordRepository recordRepo,
            QrTokenService qrTokenService,
            QrImageService qrImageService,
            StudentRepository studentRepo,
            TeacherRepository teacherRepo
    ) {
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
        this.qrTokenService = qrTokenService;
        this.qrImageService = qrImageService;
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
    }

    @Transactional
    public CreateSessionResponse createSession(String teacherUid, CreateSessionRequest req) {
        if (req == null || req.title() == null || req.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak tytułu sesji");
        }

        var teacher = teacherRepo.findByClerkUserId(teacherUid).orElseThrow(() -> 
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Nauczyciel nie znaleziony")
        );

        AttendanceSession s = new AttendanceSession();
        s.setTeacher(teacher);
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
        var teacher = teacherRepo.findByClerkUserId(teacherUid).orElseThrow(() -> 
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Nauczyciel nie znaleziony")
        );

        AttendanceSession s = sessionRepo.findByIdAndTeacher(sessionId, teacher)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sesja nie istnieje lub nie jest Twoja"
                ));

        if (!s.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sesja nieaktywna");
        }
        if (s.getShortCode() == null || s.getShortCode().isBlank()) {
            s.setShortCode(generateShortCode());
            s = sessionRepo.save(s);
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

        var student = studentRepo.findByClerkUserId(studentUid).orElseThrow(() -> 
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Student nie znaleziony")
        );
        String name = student.getName();
        String index = student.getIndexNumber();

        var existing = recordRepo.findBySessionIdAndStudent(s.getId(), student);
        if (existing.isPresent()) {
            AttendanceRecord existingRecord = existing.get();

            return new CheckInResponse(
                    existingRecord.getSessionId(),
                    s.getTitle(),
                    studentUid,
                    name,
                    index,
                    existingRecord.getCheckedInAt(),
                    true,
                    s.getEndsAt()
            );
        }

        AttendanceRecord r = new AttendanceRecord();
        r.setSessionId(s.getId());
        r.setStudent(student);
        r.setCheckedInAt(Instant.now());
        r = recordRepo.save(r);

        return new CheckInResponse(
                r.getSessionId(),
                s.getTitle(),
                studentUid,
                name,
                index,
                r.getCheckedInAt(),
                false,
                s.getEndsAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> getRecordsForSession(String teacherUid, Long sessionId) {
        var teacher = teacherRepo.findByClerkUserId(teacherUid).orElseThrow(() -> 
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Nauczyciel nie znaleziony")
        );

        AttendanceSession session = sessionRepo.findByIdAndTeacher(sessionId, teacher)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sesja nie istnieje lub nie jest Twoja"
                ));

        return recordRepo.findBySessionId(sessionId)
                .stream()
                .map(r -> {
                    Student student = r.getStudent();
                    String name = student != null ? student.getName() : "Unknown";
                    String index = student != null ? student.getIndexNumber() : null;
                    String uid = student != null ? student.getClerkUserId() : null;

                    return new CheckInResponse(
                            r.getSessionId(),
                            session.getTitle(),
                            uid,
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