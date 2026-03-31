package com.notus.backend.schedule;

import com.notus.backend.users.Role;
import com.notus.backend.users.UserDto;
import com.notus.backend.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserService userService;

    @GetMapping("/today")
    public List<Schedule> getTodaySchedule(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) Long groupId
    ) {
        return scheduleService.getTodaySchedule(teacherId, teacherName, groupId);
    }

    @GetMapping("/teacher/today")
    public List<Schedule> getTeacherTodaySchedule(
            Authentication auth,
            HttpServletRequest request
    ) {
        String uid = (String) auth.getPrincipal();
        String email = (String) request.getAttribute("clerk_email");
        String name = (String) request.getAttribute("clerk_name");

        UserDto user = userService.findOrCreate(uid, email, name);

        if (user.role() != Role.TEACHER && user.role() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tylko nauczyciel może pobrać swój plan"
            );
        }

        return scheduleService.getTodaySchedule(user.id(), null, null);
    }

    @GetMapping("/by-day")
    public List<Schedule> getScheduleByDay(@RequestParam String date) {
        return scheduleService.getScheduleByDay(LocalDate.parse(date));
    }

    @GetMapping
    public List<Schedule> getSchedule(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) Long groupId
    ) {
        return scheduleService.getSchedule(
                Instant.parse(start),
                Instant.parse(end),
                teacherId,
                teacherName,
                groupId
        );
    }
}