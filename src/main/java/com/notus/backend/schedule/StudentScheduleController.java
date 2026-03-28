package com.notus.backend.schedule;

import com.notus.backend.users.Role;
import com.notus.backend.users.Student;
import com.notus.backend.users.UserDto;
import com.notus.backend.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StudentScheduleController {

    private final ScheduleService scheduleService;
    private final UserService userService;

    @GetMapping("/api/student/schedule")
    public List<Schedule> getStudentSchedule(
            Authentication auth,
            HttpServletRequest request
    ) {
        String uid = (String) auth.getPrincipal();
        String email = (String) request.getAttribute("clerk_email");
        String name = (String) request.getAttribute("clerk_name");

        UserDto user = userService.findOrCreate(uid, email, name);

        if (user.role() != Role.STUDENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tylko student może pobrać swój plan"
            );
        }

        Student student = userService.findStudentWithGroupsByUid(uid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nie znaleziono studenta dla zalogowanego użytkownika"
                ));

        return scheduleService.getScheduleForStudent(student);
    }

    @GetMapping("/api/student/schedule/today")
    public List<Schedule> getStudentTodaySchedule(
            Authentication auth,
            HttpServletRequest request
    ) {
        String uid = (String) auth.getPrincipal();
        String email = (String) request.getAttribute("clerk_email");
        String name = (String) request.getAttribute("clerk_name");

        UserDto user = userService.findOrCreate(uid, email, name);

        if (user.role() != Role.STUDENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tylko student może pobrać swój dzisiejszy plan"
            );
        }

        Student student = userService.findStudentWithGroupsByUid(uid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nie znaleziono studenta dla zalogowanego użytkownika"
                ));

        return scheduleService.getTodayScheduleForStudent(student);
    }
}