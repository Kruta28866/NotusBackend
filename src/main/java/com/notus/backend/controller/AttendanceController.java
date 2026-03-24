package com.notus.backend.controller;

import com.notus.backend.attendance.AttendanceService;
import com.notus.backend.attendance.dto.*;
import com.notus.backend.users.Role;
import com.notus.backend.users.User;
import com.notus.backend.users.UserRepository;
import com.notus.backend.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final UserService userService;

    public AttendanceController(AttendanceService attendanceService,
                                UserRepository userRepository,
                                UserService userService) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    private User resolveUser(Authentication auth, HttpServletRequest request) {
        String uid = (String) auth.getPrincipal();
        String email = (String) request.getAttribute("clerk_email");

        userService.findOrCreate(uid, email, "User");

        return userRepository.findByClerkUserId(uid)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie istnieje"));
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSessionResponse createSession(Authentication auth,
                                               HttpServletRequest request,
                                               @RequestBody CreateSessionRequest req) {
        String uid = (String) auth.getPrincipal();
        User u = resolveUser(auth, request);

        if (u.getRole() != Role.TEACHER && u.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Tylko TEACHER/ADMIN może tworzyć sesje");
        }

        return attendanceService.createSession(uid, req);
    }

    @GetMapping("/sessions/{sessionId}/qr")
    public QrResponse getQr(Authentication auth,
                            HttpServletRequest request,
                            @PathVariable Long sessionId) {
        String uid = (String) auth.getPrincipal();
        User u = resolveUser(auth, request);

        if (u.getRole() != Role.TEACHER && u.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Tylko TEACHER/ADMIN może generować QR");
        }

        return attendanceService.generateQr(uid, sessionId);
    }

    @PostMapping("/check-in")
    public CheckInResponse checkIn(Authentication auth,
                                   HttpServletRequest request,
                                   @RequestBody CheckInRequest req) {
        String uid = (String) auth.getPrincipal();
        User u = resolveUser(auth, request);

        if (u.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Tylko STUDENT może robić check-in");
        }

        return attendanceService.checkIn(uid, req);
    }

    @GetMapping("/sessions/{id}/records")
    public List<CheckInResponse> getRecords(Authentication auth,
                                            HttpServletRequest request,
                                            @PathVariable Long id) {
        String uid = (String) auth.getPrincipal();
        User u = resolveUser(auth, request);

        if (u.getRole() != Role.TEACHER && u.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Tylko TEACHER/ADMIN może przeglądać obecności");
        }

        return attendanceService.getRecordsForSession(uid, id);
    }
}