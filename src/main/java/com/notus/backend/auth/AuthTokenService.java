package com.notus.backend.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class AuthTokenService {

    private static final String ISSUER = "notus-local-auth";

    private final Algorithm algorithm;

    public AuthTokenService(@Value("${notus.auth.jwt-secret:local-dev-secret-change-me}") String jwtSecret) {
        this.algorithm = Algorithm.HMAC256(jwtSecret);
    }

    public String issueLocalToken(String authUserId, String email) {
        Instant expiresAt = Instant.now().plusSeconds(60 * 60 * 8);
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(authUserId)
                .withClaim("email", email)
                .withClaim("provider", "local")
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public DecodedJWT verifyLocalToken(String token) {
        return JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
                .verify(token);
    }
}
