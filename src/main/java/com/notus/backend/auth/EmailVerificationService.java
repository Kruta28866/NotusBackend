package com.notus.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    public void sendVerificationEmail(String email, String verificationToken) {
        log.info("Teacher email verification for {}: /api/auth/teacher/verify-email token={}", email, verificationToken);
    }
}
