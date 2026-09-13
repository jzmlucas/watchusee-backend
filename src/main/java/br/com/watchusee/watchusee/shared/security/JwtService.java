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
    private static final String DEFAULT_AUDIENCE = "watchusee-api";

    private final SecretKey secretKey;
    private final long expirationMillis;
    private final String issuer;
    private final String audience;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMillis,
            @Value("${security.jwt.issuer:" + DEFAULT_ISSUER + "}") String issuer,
            @Value("${security.jwt.audience:" + DEFAULT_AUDIENCE + "}") String audience,
            TokenBlacklistService tokenBlacklistService
    ) {

        this.secretKey = createSecretKey(secret);
        this.expirationMillis = expirationMillis;
        this.issuer = issuer;
        this.audience = audience;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public String generateToken(Long userId) {

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .audience()
                .add(audience)
                .and()
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Long extractUserId(String token) {

        Claims claims = parseClaims(token);

        return Long.valueOf(
                claims.getSubject()
        );
    }

    public Instant extractIssuedAt(String token) {

        Claims claims = parseClaims(token);

        Date issuedAt = claims.getIssuedAt();

        return issuedAt != null
                ? issuedAt.toInstant()
                : null;
    }

    public boolean isValid(String token) {

        try {

            Claims claims = parseClaims(token);

            String tokenId = claims.getId();

            return tokenId != null
                    && !tokenBlacklistService.isRevoked(tokenId);

        } catch (JwtException | IllegalArgumentException exception) {

            log.debug(
                    "Token JWT inválido: {}",
                    exception.getMessage()
            );

            return false;
        }
    }

    public void revoke(String token) {

        try {

            Claims claims = parseClaims(token);

            String tokenId = claims.getId();
            Date expiration = claims.getExpiration();

            if (tokenId == null) {
                return;
            }

            Instant expiresAt = expiration != null
                    ? expiration.toInstant()
                    : Instant.now();

            tokenBlacklistService.revoke(
                    tokenId,
                    expiresAt
            );

        } catch (JwtException | IllegalArgumentException exception) {

            log.debug(
                    "Tentativa de revogar um token JWT inválido: {}",
                    exception.getMessage()
            );
        }
    }

    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey createSecretKey(String secret) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "A chave JWT não pode ser nula ou vazia."
            );
        }

        byte[] keyBytes =
                secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "A chave JWT deve possuir pelo menos 32 bytes."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}