package com.notus.backend.controller;

import com.notus.backend.attendance.AttendanceService;
import com.notus.backend.attendance.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    // Teacher: tworzy sesję
    @PostMapping("/sessions")
    public com.notus.backend.attendance.dto.CreateSessionResponse createSession(Authentication auth, @RequestBody com.notus.backend.attendance.dto.CreateSessionRequest req) {
        String uid = (String) auth.getPrincipal(); // z FirebaseAuthFilter
        return service.createSession(uid, req);
    }

    // Teacher: generuje QR dla sesji
    @PostMapping("/sessions/{id}/qr")
    public QrResponse generateQr(Authentication auth, @PathVariable("id") Long sessionId) {
        String uid = (String) auth.getPrincipal();
        return service.generateQr(uid, sessionId);
    }

    // Student: check-in po zeskanowaniu QR
    @PostMapping("/check-in")
    public com.notus.backend.attendance.dto.CheckInResponse checkIn(Authentication auth, @RequestBody com.notus.backend.attendance.dto.CheckInRequest req) {
        String uid = (String) auth.getPrincipal();
        return service.checkIn(uid, req);
    }
}
