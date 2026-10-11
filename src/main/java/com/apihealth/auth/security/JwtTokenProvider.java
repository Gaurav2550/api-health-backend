package com.apihealth.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {
        SecretKey secretKey;
        if (jwtSecret.length() >= 64) {
            secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        } else {
            try {
                secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
            } catch (Exception e) {
                secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            }
        }
        this.key = secretKey;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.debug("JWT unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.debug("JWT malformed: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.debug("JWT signature invalid: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.debug("JWT claims string empty: {}", ex.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
