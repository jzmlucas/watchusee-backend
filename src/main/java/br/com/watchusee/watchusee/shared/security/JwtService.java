package br.com.watchusee.watchusee.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log =
            LoggerFactory.getLogger(JwtService.class);

    private static final String DEFAULT_ISSUER = "watchusee-api";

    private final SecretKey secretKey;
    private final long expirationMillis;
    private final String issuer;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMillis,
            @Value("${security.jwt.issuer:" + DEFAULT_ISSUER + "}") String issuer,
            TokenBlacklistService tokenBlacklistService
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
        this.issuer = issuer;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public String generateToken(Long userId) {

        Instant now = Instant.now();

        Instant expiration =
                now.plusMillis(expirationMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
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

    public Instant extractIssuedAt(String token) {

        Claims claims =
                parseClaims(token);

        Date issuedAt = claims.getIssuedAt();

        return issuedAt != null ? issuedAt.toInstant() : null;
    }

    public boolean isValid(String token) {

        try {

            Claims claims =
                    parseClaims(token);

            if (tokenBlacklistService.isRevoked(claims.getId())) {
                return false;
            }

            return true;

        } catch (JwtException | IllegalArgumentException exception) {

            return false;
        }
    }

    public void revoke(String token) {

        try {

            Claims claims = parseClaims(token);

            tokenBlacklistService.revoke(
                    claims.getId(),
                    claims.getExpiration() != null
                            ? claims.getExpiration().toInstant()
                            : Instant.now()
            );

        } catch (JwtException | IllegalArgumentException exception) {

            log.debug("Tentativa de revogar um token inválido.");
        }
    }

    private Claims parseClaims(String token) {

        return Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
