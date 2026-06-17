package com.example.eventfinder.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long tokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:${JWT_SECRET:default-secret-key-for-development-only-min-32-chars}}") String secret,
            @Value("${app.jwt.expiration:604800000}") long tokenExpiration) {
        // Ensure secret is at least 32 characters for HS256
        String finalSecret = secret;
        if (secret.length() < 32) {
            finalSecret = secret + "0".repeat(Math.max(0, 32 - secret.length()));
        }
        this.secretKey = Keys.hmacShaKeyFor(finalSecret.getBytes());
        this.tokenExpirationMs = tokenExpiration;
    }

    /**
     * Generate JWT token for a given username
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

 /**
 * Extract username from JWT token
 */
public String extractUsername(String token) {
    try {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    } catch (JwtException | IllegalArgumentException e) {
        return null;
    }
}

/**
 * Validate JWT token
 */
public boolean validateToken(String token) {
    try {
        Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;
    }
}

    /**
     * Get username from Authorization header (format: "Bearer <token>")
     */
    public String getUsernameFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return extractUsername(token);
    }

    /**
     * Validate Authorization header token
     */
    public boolean validateHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return validateToken(token);
    }
}
