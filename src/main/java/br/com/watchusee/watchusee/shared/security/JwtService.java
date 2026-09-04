package br.com.watchusee.watchusee.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMillis
    ) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "A chave JWT deve possuir pelo menos 32 caracteres."
            );
        }

        this.secretKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                );

        this.expirationMillis = expirationMillis;
    }

    public String generateToken(Long userId) {

        Instant now = Instant.now();

        Instant expiration =
                now.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public Long extractUserId(String token) {

        Claims claims =
                parseClaims(token);

        return Long.valueOf(
                claims.getSubject()
        );
    }

    public boolean isValid(String token) {

        try {

            parseClaims(token);

            return true;

        } catch (Exception exception) {

            return false;
        }
    }

    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}