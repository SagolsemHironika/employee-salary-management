package com.acme.salary.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-at-least-32-bytes-long!!";

    private final JwtService jwtService = new JwtService(SECRET, 480);

    @Test
    void generatedTokenRoundTripsToOriginalEmailAndRole() {
        String token = jwtService.generateToken("admin@acme.example", "admin");

        Optional<Claims> claims = jwtService.parse(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("admin@acme.example");
        assertThat(jwtService.role(claims.get())).isEqualTo("admin");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken("admin@acme.example", "admin");
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-key-32-bytes!", 480);
        String token = otherService.generateToken("admin@acme.example", "admin");

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expiredTokenService = new JwtService(SECRET, -1);
        String token = expiredTokenService.generateToken("admin@acme.example", "admin");

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
    }
}
