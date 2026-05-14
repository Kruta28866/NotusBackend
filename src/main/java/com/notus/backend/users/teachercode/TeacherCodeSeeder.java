package com.notus.backend.users.teachercode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TeacherCodeSeeder implements CommandLineRunner {

    private final TeacherCodeService teacherCodeService;
    private final TeacherCodeRepository teacherCodeRepository;
    private final String initialTeacherCode;

    public TeacherCodeSeeder(TeacherCodeService teacherCodeService,
                             TeacherCodeRepository teacherCodeRepository,
                             @Value("${notus.auth.initial-teacher-code:}") String initialTeacherCode) {
        this.teacherCodeService = teacherCodeService;
        this.teacherCodeRepository = teacherCodeRepository;
        this.initialTeacherCode = initialTeacherCode;
    }

    @Override
    public void run(String... args) {
        if (initialTeacherCode == null || initialTeacherCode.isBlank()) {
            log.info("INITIAL_TEACHER_CODE is not configured, skipping teacher code seeding");
            return;
        }

        String normalizedCode = initialTeacherCode.trim().toUpperCase();
        if (teacherCodeRepository.existsByCode(normalizedCode)) {
            log.info("Initial teacher code already exists");
            return;
        }

        teacherCodeService.generateCode(normalizedCode, null, null);
        log.info("Initial teacher code seeded");
    }
}
