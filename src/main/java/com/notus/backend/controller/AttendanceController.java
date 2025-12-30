package com.notus.backend.controller;

import com.notus.backend.attendance.AttendanceService;
import com.notus.backend.attendance.dto.CheckInRequest;
import com.notus.backend.attendance.dto.CheckInResponse;
import com.notus.backend.attendance.dto.CreateSessionRequest;
import com.notus.backend.attendance.dto.CreateSessionResponse;
import com.notus.backend.attendance.dto.QrResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // teacher: tworzy sesję
    @PostMapping("/sessions")
    public CreateSessionResponse createSession(Authentication auth, @RequestBody CreateSessionRequest req) {
        String uid = (String) auth.getPrincipal();
        return attendanceService.createSession(uid, req);
    }

    // teacher: generuje QR
    @GetMapping("/sessions/{sessionId}/qr")
    public QrResponse qr(Authentication auth, @PathVariable Long sessionId) {
        String uid = (String) auth.getPrincipal();
        return attendanceService.generateQr(uid, sessionId);
    }

    // student: check-in po zeskanowaniu QR
    @PostMapping("/check-in")
    public CheckInResponse checkIn(Authentication auth, @RequestBody CheckInRequest req) {
        String uid = (String) auth.getPrincipal();
        return attendanceService.checkIn(uid, req);
    }
}
