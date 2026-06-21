package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UserDetailsImpl userDetails) {
        logger.debug("Generating JWT token for user: {}", userDetails.getUsername());
        String token = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", userDetails.getUser().getRole().getName().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
        logger.debug("JWT token generated successfully for user: {}", userDetails.getUsername());
        return token;
    }

    public String extractEmail(String token) {
        logger.debug("Extracting email from JWT token");
        String email = getClaims(token).getSubject();
        logger.debug("Email extracted: {}", email);
        return email;
    }

    public boolean isValid(String token) {
        try {
            getClaims(token);
            logger.debug("JWT token is valid");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
}
