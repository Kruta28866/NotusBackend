package com.notus.backend.auth;

import com.notus.backend.auth.dto.LoginRequest;
import com.notus.backend.auth.dto.StudentRegisterRequest;
import com.notus.backend.auth.dto.TeacherAuthResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/student")
public class StudentAuthController {

    private final StudentRegistrationService studentRegistrationService;

    public StudentAuthController(StudentRegistrationService studentRegistrationService) {
        this.studentRegistrationService = studentRegistrationService;
    }

    @PostMapping("/register")
    public TeacherAuthResponse registerStudent(@RequestBody StudentRegisterRequest request) {
        return studentRegistrationService.register(request);
    }

    @PostMapping("/login")
    public TeacherAuthResponse loginStudent(@RequestBody LoginRequest request) {
        return studentRegistrationService.login(request);
    }
}
