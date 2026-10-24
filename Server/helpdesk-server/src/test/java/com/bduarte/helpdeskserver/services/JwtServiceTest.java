package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.models.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static com.bduarte.helpdeskserver.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-bytes!!";

    private JwtService jwtService;
    private UserDetailsImpl agent;

    @BeforeEach
    void setUp() {
        jwtService = jwtService(SECRET, 3_600_000L);
        agent = new UserDetailsImpl(user(Role.RoleName.ROLE_AGENT, "agent@helpdesk.com"));
    }

    @Test
    void generatedTokenCarriesEmailRoleAndExpiration() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken(agent);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo("agent@helpdesk.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_AGENT");
        // JWT dates have second precision
        assertThat(claims.getExpiration()).isBetween(
                new Date(before + 3_600_000L - 1_000L), new Date(System.currentTimeMillis() + 3_600_000L), true, true);
    }

    @Test
    void extractEmailReturnsSubject() {
        assertThat(jwtService.extractEmail(jwtService.generateToken(agent))).isEqualTo("agent@helpdesk.com");
    }

    @Test
    void extractEmailRejectsInvalidToken() {
        assertThatThrownBy(() -> jwtService.extractEmail("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void ownTokenIsValid() {
        assertThat(jwtService.isValid(jwtService.generateToken(agent))).isTrue();
    }

    @Test
    void tokenSignedWithAnotherSecretIsInvalid() {
        String foreignToken = jwtService("another-secret-with-at-least-thirty-two-bytes", 3_600_000L).generateToken(agent);

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void tamperedTokenIsInvalid() {
        String token = jwtService.generateToken(agent);
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin@helpdesk.com\",\"role\":\"ROLE_ADMIN\"}".getBytes());

        assertThat(jwtService.isValid(parts[0] + "." + forgedPayload + "." + parts[2])).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        String expired = jwtService(SECRET, -60_000L).generateToken(agent);

        assertThat(jwtService.isValid(expired)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"garbage", "a.b.c"})
    void malformedTokenIsInvalid(String token) {
        assertThat(jwtService.isValid(token)).isFalse();
    }

    private static JwtService jwtService(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationMs", expirationMs);
        return service;
    }
}
