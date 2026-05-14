package com.notus.backend.auth;

import com.notus.backend.auth.dto.EmailVerificationRequest;
import com.notus.backend.auth.dto.LoginRequest;
import com.notus.backend.auth.dto.TeacherAuthResponse;
import com.notus.backend.auth.dto.TeacherGoogleRegisterRequest;
import com.notus.backend.auth.dto.TeacherRegisterRequest;
import com.notus.backend.auth.dto.TeacherVerifyCodeRequest;
import com.notus.backend.auth.dto.TeacherVerifyCodeResponse;
import com.notus.backend.users.teachercode.TeacherCodeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class TeacherAuthController {

    private final TeacherCodeService teacherCodeService;
    private final TeacherRegistrationService teacherRegistrationService;

    public TeacherAuthController(TeacherCodeService teacherCodeService,
                                 TeacherRegistrationService teacherRegistrationService) {
        this.teacherCodeService = teacherCodeService;
        this.teacherRegistrationService = teacherRegistrationService;
    }

    @PostMapping("/teacher/verify-code")
    public TeacherVerifyCodeResponse verifyTeacherCode(@RequestBody TeacherVerifyCodeRequest request) {
        return new TeacherVerifyCodeResponse(true, teacherCodeService.verifyCodeForRegistration(request.code(), request.email()));
    }

    @PostMapping("/teacher/register")
    public TeacherAuthResponse registerTeacher(@RequestBody TeacherRegisterRequest request) {
        return teacherRegistrationService.registerWithEmail(request);
    }

    @PostMapping("/teacher/google-register")
    public TeacherAuthResponse registerOrLoginTeacherWithGoogle(@RequestBody TeacherGoogleRegisterRequest request) {
        return teacherRegistrationService.registerOrLoginWithGoogle(request);
    }

    @PostMapping("/teacher/login")
    public TeacherAuthResponse loginTeacher(@RequestBody LoginRequest request) {
        return teacherRegistrationService.login(request);
    }

    @PostMapping("/teacher/verify-email")
    public TeacherAuthResponse verifyTeacherEmail(@RequestBody EmailVerificationRequest request) {
        return teacherRegistrationService.verifyEmail(request);
    }
}
