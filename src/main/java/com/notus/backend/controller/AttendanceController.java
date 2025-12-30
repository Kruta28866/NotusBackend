package com.notus.backend.controller;

import com.notus.backend.attendance.AttendanceService;
import com.notus.backend.attendance.dto.*;
import com.notus.backend.users.Role;
import com.notus.backend.users.User;
import com.notus.backend.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    public AttendanceController(AttendanceService attendanceService, UserRepository userRepository) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
    }

    // 1) teacher tworzy sesję
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSessionResponse createSession(Authentication auth, @RequestBody CreateSessionRequest req) {
        String uid = (String) auth.getPrincipal();

        User u = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new IllegalStateException("Brak usera w bazie dla uid=" + uid));

        if (u.getRole() != Role.TEACHER && u.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Tylko TEACHER/ADMIN może tworzyć sesje");
        }

        return attendanceService.createSession(uid, req);
    }

    // 2) teacher generuje QR dla sesji
    @GetMapping("/sessions/{sessionId}/qr")
    public QrResponse getQr(Authentication auth, @PathVariable Long sessionId) {
        String uid = (String) auth.getPrincipal();

        User u = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new IllegalStateException("Brak usera w bazie dla uid=" + uid));

        if (u.getRole() != Role.TEACHER && u.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Tylko TEACHER/ADMIN może generować QR");
        }

        return attendanceService.generateQr(uid, sessionId);
    }

    // 3) student robi check-in skanując QR (token z QR)
    @PostMapping("/check-in")
    public CheckInResponse checkIn(Authentication auth, @RequestBody CheckInRequest req) {
        String uid = (String) auth.getPrincipal();

        User u = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new IllegalStateException("Brak usera w bazie dla uid=" + uid));

        if (u.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Tylko STUDENT może robić check-in");
        }

        return attendanceService.checkIn(uid, req);
    }
}
