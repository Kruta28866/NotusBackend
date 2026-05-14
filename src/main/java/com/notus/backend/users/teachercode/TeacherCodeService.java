package com.notus.backend.users.teachercode;

import com.notus.backend.auth.HashService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class TeacherCodeService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GENERATED_CODE_LENGTH = 10;

    private final TeacherCodeRepository teacherCodeRepository;
    private final TeacherRegistrationTokenRepository registrationTokenRepository;
    private final HashService hashService;
    private final long registrationTokenTtlSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public TeacherCodeService(TeacherCodeRepository teacherCodeRepository,
                              TeacherRegistrationTokenRepository registrationTokenRepository,
                              HashService hashService,
                              @Value("${notus.auth.teacher-registration-token-ttl-seconds:900}") long registrationTokenTtlSeconds) {
        this.teacherCodeRepository = teacherCodeRepository;
        this.registrationTokenRepository = registrationTokenRepository;
        this.hashService = hashService;
        this.registrationTokenTtlSeconds = registrationTokenTtlSeconds;
    }

    @Transactional(readOnly = true)
    public void validateCode(String rawCode) {
        TeacherCode teacherCode = findByRawCode(rawCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Niepoprawny kod dostępu nauczyciela"));

        assertUsable(teacherCode, false);
    }

    @Transactional
    public String verifyCodeForRegistration(String rawCode, String email) {
        TeacherCode teacherCode = findByRawCode(rawCode).orElseThrow(this::invalidCode);
        assertUsable(teacherCode, true);
        assertEmailMatches(teacherCode, email, true);

        String token = generateUrlToken();
        TeacherRegistrationToken registrationToken = new TeacherRegistrationToken();
        registrationToken.setTokenHash(hashService.sha256(token));
        registrationToken.setEmail(normalizeEmail(email));
        registrationToken.setTeacherCode(teacherCode);
        registrationToken.setExpiresAt(Instant.now().plusSeconds(registrationTokenTtlSeconds));
        registrationTokenRepository.save(registrationToken);

        return token;
    }

    @Transactional
    public TeacherCode consumeRegistrationToken(String rawToken, String email, String usedByUserId) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidCode();
        }

        TeacherRegistrationToken registrationToken = registrationTokenRepository.findByTokenHash(hashService.sha256(rawToken))
                .orElseThrow(this::invalidCode);

        if (registrationToken.getUsedAt() != null || registrationToken.getExpiresAt() == null || !registrationToken.getExpiresAt().isAfter(Instant.now())) {
            throw invalidCode();
        }

        String normalizedEmail = normalizeEmail(email);
        if (registrationToken.getEmail() != null && normalizedEmail != null && !registrationToken.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw invalidCode();
        }

        TeacherCode teacherCode = registrationToken.getTeacherCode();
        assertUsable(teacherCode, true);
        assertEmailMatches(teacherCode, email, true);

        registrationToken.setUsedAt(Instant.now());
        incrementUsage(teacherCode, usedByUserId);
        registrationTokenRepository.save(registrationToken);
        return teacherCodeRepository.save(teacherCode);
    }

    @Transactional
    public TeacherCodeDto generateCode(String rawCode, Instant expiresAt, Integer usageLimit) {
        validateUsageLimit(usageLimit);

        String code = rawCode == null || rawCode.isBlank()
                ? generateUniqueCode()
                : normalize(rawCode);

        String codeHash = hashService.sha256(code);
        if (teacherCodeRepository.existsByCode(code) || teacherCodeRepository.existsByCodeHash(codeHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kod nauczyciela już istnieje");
        }

        TeacherCode teacherCode = new TeacherCode();
        teacherCode.setCode(code);
        teacherCode.setCodeHash(codeHash);
        teacherCode.setActive(true);
        teacherCode.setExpiresAt(expiresAt);
        teacherCode.setUsageLimit(usageLimit);

        return TeacherCodeDto.from(teacherCodeRepository.save(teacherCode));
    }

    @Transactional
    public TeacherCodeDto deactivateCode(Long id) {
        TeacherCode teacherCode = teacherCodeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono kodu nauczyciela"));
        teacherCode.setActive(false);
        return TeacherCodeDto.from(teacherCodeRepository.save(teacherCode));
    }

    @Transactional
    public void consumeCode(String rawCode) {
        TeacherCode teacherCode = teacherCodeRepository.findByCodeForUpdate(normalize(rawCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Niepoprawny kod dostępu nauczyciela"));

        assertUsable(teacherCode, false);
        incrementUsage(teacherCode, null);
        teacherCodeRepository.save(teacherCode);
    }

    @Transactional(readOnly = true)
    public List<TeacherCodeDto> listActiveCodes() {
        return teacherCodeRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .filter(teacherCode -> !isExpired(teacherCode))
                .filter(teacherCode -> !isUsageLimitReached(teacherCode))
                .map(TeacherCodeDto::from)
                .toList();
    }

    public boolean isExpired(TeacherCode teacherCode) {
        return teacherCode.getExpiresAt() != null && !teacherCode.getExpiresAt().isAfter(Instant.now());
    }

    public boolean isUsageLimitReached(TeacherCode teacherCode) {
        return teacherCode.getUsageLimit() != null && teacherCode.getTimesUsed() >= teacherCode.getUsageLimit();
    }

    private void assertUsable(TeacherCode teacherCode, boolean genericError) {
        if (!teacherCode.isActive()) {
            if (genericError) throw invalidCode();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kod nauczyciela jest nieaktywny");
        }

        if (isExpired(teacherCode)) {
            if (genericError) throw invalidCode();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kod nauczyciela wygasł");
        }

        if (teacherCode.isUsed() || isUsageLimitReached(teacherCode)) {
            if (genericError) throw invalidCode();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Limit użyć kodu nauczyciela został wyczerpany");
        }
    }

    private void assertEmailMatches(TeacherCode teacherCode, String email, boolean genericError) {
        if (teacherCode.getEmail() == null || teacherCode.getEmail().isBlank()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || !teacherCode.getEmail().equalsIgnoreCase(normalizedEmail)) {
            if (genericError) throw invalidCode();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kod nauczyciela jest przypisany do innego adresu email");
        }
    }

    private void incrementUsage(TeacherCode teacherCode, String usedByUserId) {
        teacherCode.setTimesUsed(teacherCode.getTimesUsed() + 1);

        if (teacherCode.getUsageLimit() != null && teacherCode.getTimesUsed() >= teacherCode.getUsageLimit()) {
            teacherCode.setActive(false);
            teacherCode.setUsed(true);
            teacherCode.setUsedAt(Instant.now());
            teacherCode.setUsedByUserId(usedByUserId);
        }
    }

    private java.util.Optional<TeacherCode> findByRawCode(String rawCode) {
        String normalizedCode = normalize(rawCode);
        return teacherCodeRepository.findByCodeHash(hashService.sha256(normalizedCode))
                .or(() -> teacherCodeRepository.findByCode(normalizedCode));
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Kod administratora jest nieprawidłowy albo wygasł");
    }

    private String normalize(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kod dostępu nauczyciela jest wymagany");
        }

        return rawCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return null;
        }
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private void validateUsageLimit(Integer usageLimit) {
        if (usageLimit != null && usageLimit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit użyć musi być większy od zera");
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = generateRandomCode();
            if (!teacherCodeRepository.existsByCode(code) && !teacherCodeRepository.existsByCodeHash(hashService.sha256(code))) {
                return code;
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nie udało się wygenerować unikalnego kodu");
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(GENERATED_CODE_LENGTH);
        for (int i = 0; i < GENERATED_CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private String generateUrlToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
