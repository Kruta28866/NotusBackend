package com.notus.backend.controller;

import com.notus.backend.attendance.AttendanceService;
import com.notus.backend.attendance.dto.*;
import com.notus.backend.users.Role;
import com.notus.backend.users.UserDto;
import com.notus.backend.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserService userService;

    public AttendanceController(AttendanceService attendanceService,
                                UserService userService) {
        this.attendanceService = attendanceService;
        this.userService = userService;
    }

    private UserDto resolveUser(Authentication auth, HttpServletRequest request) {
        String uid = (String) auth.getPrincipal();
        String email = (String) request.getAttribute("clerk_email");
        String name = (String) request.getAttribute("clerk_name");

        return userService.findOrCreate(uid, email, name);
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSessionResponse createSession(Authentication auth,
                                               HttpServletRequest request,
                                               @RequestBody CreateSessionRequest req) {
        String uid = (String) auth.getPrincipal();
        UserDto u = resolveUser(auth, request);

        if (u.role() != Role.TEACHER && u.role() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tylko TEACHER/ADMIN może tworzyć sesje");
        }

        return attendanceService.createSession(uid, req);
    }

    @GetMapping("/sessions/{sessionId}/qr")
    public QrResponse getQr(Authentication auth,
                            HttpServletRequest request,
                            @PathVariable Long sessionId) {
        String uid = (String) auth.getPrincipal();
        UserDto u = resolveUser(auth, request);

        if (u.role() != Role.TEACHER && u.role() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tylko TEACHER/ADMIN może generować QR");
        }

        return attendanceService.generateQr(uid, sessionId);
    }

    @PostMapping("/check-in")
    public CheckInResponse checkIn(Authentication auth,
                                   HttpServletRequest request,
                                   @RequestBody CheckInRequest req) {
        String uid = (String) auth.getPrincipal();
        UserDto u = resolveUser(auth, request);

        if (u.role() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tylko STUDENT może robić check-in");
        }

        return attendanceService.checkIn(uid, req);
    }

    @GetMapping("/sessions/{id}/records")
    public List<CheckInResponse> getRecords(Authentication auth,
                                            HttpServletRequest request,
                                            @PathVariable Long id) {
        String uid = (String) auth.getPrincipal();
        UserDto u = resolveUser(auth, request);

        if (u.role() != Role.TEACHER && u.role() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tylko TEACHER/ADMIN może przeglądać obecności");
        }

        return attendanceService.getRecordsForSession(uid, id);
    }
}