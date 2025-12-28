package com.notus.backend.attendance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class QrTokenService {

    private final String hmacSecret;
    private final long ttlSeconds;
    private final SecureRandom random = new SecureRandom();

    public QrTokenService(
            @Value("${notus.qr.hmacSecret}") String hmacSecret,
            @Value("${notus.qr.ttlSeconds:60}") long ttlSeconds
    ) {
        this.hmacSecret = hmacSecret;
        this.ttlSeconds = ttlSeconds;
    }

    public record TokenData(Long sessionId, long exp, String nonce) {}

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public String createToken(Long sessionId) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String nonce = randomNonce(16);

        String payload = sessionId + "." + exp + "." + nonce;
        String sig = hmacSha256Base64Url(payload);

        return payload + "." + sig;
    }

    public TokenData verifyAndParse(String token) {
        if (token == null) throw new IllegalArgumentException("Brak tokenu QR");

        String[] parts = token.split("\\.");
        if (parts.length != 4) throw new IllegalArgumentException("Zły format tokenu QR");

        long sessionId = Long.parseLong(parts[0]);
        long exp = Long.parseLong(parts[1]);
        String nonce = parts[2];
        String sig = parts[3];

        String payload = parts[0] + "." + parts[1] + "." + parts[2];
        String expected = hmacSha256Base64Url(payload);

        if (!constantTimeEquals(sig, expected)) {
            throw new IllegalArgumentException("Niepoprawny podpis QR");
        }

        long now = Instant.now().getEpochSecond();
        if (now > exp) {
            throw new IllegalArgumentException("QR wygasł");
        }

        return new TokenData(sessionId, exp, nonce);
    }

    private String randomNonce(int bytes) {
        byte[] b = new byte[bytes];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String hmacSha256Base64Url(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }
}
