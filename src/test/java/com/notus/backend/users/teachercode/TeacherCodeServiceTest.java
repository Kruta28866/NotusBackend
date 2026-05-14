package com.notus.backend.users.teachercode;

import com.notus.backend.auth.HashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherCodeServiceTest {

    @Mock
    private TeacherCodeRepository teacherCodeRepository;

    @Mock
    private TeacherRegistrationTokenRepository registrationTokenRepository;

    private HashService hashService;

    private TeacherCodeService teacherCodeService;

    @BeforeEach
    void setUp() {
        hashService = new HashService();
        teacherCodeService = new TeacherCodeService(teacherCodeRepository, registrationTokenRepository, hashService, 900);
    }

    @Test
    void generateCodeCreatesActiveCodeWithUsageLimit() {
        when(teacherCodeRepository.existsByCode("WELCOME")).thenReturn(false);
        when(teacherCodeRepository.save(any(TeacherCode.class))).thenAnswer(invocation -> {
            TeacherCode code = invocation.getArgument(0);
            code.setId(1L);
            code.setCreatedAt(Instant.parse("2026-05-13T12:00:00Z"));
            return code;
        });

        TeacherCodeDto result = teacherCodeService.generateCode(" welcome ", null, 3);

        assertEquals("WELCOME", result.code());
        assertTrue(result.isActive());
        assertEquals(3, result.usageLimit());
        assertEquals(0, result.timesUsed());
    }

    @Test
    void consumeCodeIncrementsUsageAndDisablesWhenLimitReached() {
        TeacherCode code = activeCode("ONE-TIME");
        code.setUsageLimit(1);

        when(teacherCodeRepository.findByCodeForUpdate("ONE-TIME")).thenReturn(Optional.of(code));
        when(teacherCodeRepository.save(any(TeacherCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        teacherCodeService.consumeCode("one-time");

        ArgumentCaptor<TeacherCode> captor = ArgumentCaptor.forClass(TeacherCode.class);
        verify(teacherCodeRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getTimesUsed());
        assertFalse(captor.getValue().isActive());
    }

    @Test
    void validateCodeRejectsExpiredCode() {
        TeacherCode code = activeCode("OLD");
        code.setExpiresAt(Instant.now().minusSeconds(60));

        when(teacherCodeRepository.findByCodeHash(anyString())).thenReturn(Optional.empty());
        when(teacherCodeRepository.findByCode("OLD")).thenReturn(Optional.of(code));

        assertThrows(ResponseStatusException.class, () -> teacherCodeService.validateCode("OLD"));
    }

    @Test
    void validateCodeRejectsDisabledCode() {
        TeacherCode code = activeCode("DISABLED");
        code.setActive(false);

        when(teacherCodeRepository.findByCodeHash(anyString())).thenReturn(Optional.empty());
        when(teacherCodeRepository.findByCode("DISABLED")).thenReturn(Optional.of(code));

        assertThrows(ResponseStatusException.class, () -> teacherCodeService.validateCode("DISABLED"));
    }

    @Test
    void validateCodeRejectsUsageLimitReached() {
        TeacherCode code = activeCode("USED");
        code.setUsageLimit(2);
        code.setTimesUsed(2);

        when(teacherCodeRepository.findByCodeHash(anyString())).thenReturn(Optional.empty());
        when(teacherCodeRepository.findByCode("USED")).thenReturn(Optional.of(code));

        assertThrows(ResponseStatusException.class, () -> teacherCodeService.validateCode("USED"));
    }

    @Test
    void verifyCodeGeneratesRegistrationTokenWithoutConsumingCode() {
        TeacherCode code = activeCode("WELCOME");

        when(teacherCodeRepository.findByCodeHash(hashService.sha256("WELCOME"))).thenReturn(Optional.of(code));
        when(registrationTokenRepository.save(any(TeacherRegistrationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String token = teacherCodeService.verifyCodeForRegistration("welcome", "teacher@example.com");

        assertTrue(token.length() > 20);
        assertEquals(0, code.getTimesUsed());
        assertTrue(code.isActive());
    }

    @Test
    void consumeRegistrationTokenRejectsExpiredToken() {
        TeacherRegistrationToken token = new TeacherRegistrationToken();
        token.setTokenHash(hashService.sha256("expired-token"));
        token.setTeacherCode(activeCode("WELCOME"));
        token.setExpiresAt(Instant.now().minusSeconds(60));

        when(registrationTokenRepository.findByTokenHash(hashService.sha256("expired-token"))).thenReturn(Optional.of(token));

        assertThrows(ResponseStatusException.class, () -> teacherCodeService.consumeRegistrationToken("expired-token", "teacher@example.com", "user-1"));
    }

    @Test
    void listActiveCodesReturnsOnlyUsableCodes() {
        TeacherCode usableCode = activeCode("USABLE");
        TeacherCode expiredCode = activeCode("EXPIRED");
        expiredCode.setExpiresAt(Instant.now().minusSeconds(60));
        TeacherCode usedCode = activeCode("USED");
        usedCode.setUsageLimit(1);
        usedCode.setTimesUsed(1);

        when(teacherCodeRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(expiredCode, usedCode, usableCode));

        List<TeacherCodeDto> result = teacherCodeService.listActiveCodes();

        assertEquals(1, result.size());
        assertEquals("USABLE", result.getFirst().code());
    }

    private TeacherCode activeCode(String value) {
        TeacherCode code = new TeacherCode();
        code.setCode(value);
        code.setActive(true);
        code.setCreatedAt(Instant.now());
        return code;
    }
}
