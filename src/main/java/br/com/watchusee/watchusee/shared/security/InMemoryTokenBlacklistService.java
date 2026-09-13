package br.com.watchusee.watchusee.shared.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Instant> revokedTokens =
            new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, Instant expiresAt) {

        if (jti == null || jti.isBlank()) {
            return;
        }

        Instant revokedUntil =
                expiresAt != null
                        ? expiresAt
                        : Instant.now();

        if (!revokedUntil.isAfter(Instant.now())) {
            return;
        }

        revokedTokens.put(jti, revokedUntil);

        cleanupExpired();
    }

    @Override
    public boolean isRevoked(String jti) {

        if (jti == null || jti.isBlank()) {
            return false;
        }

        Instant revokedUntil =
                revokedTokens.get(jti);

        if (revokedUntil == null) {
            return false;
        }

        Instant now = Instant.now();

        if (!revokedUntil.isAfter(now)) {

            revokedTokens.remove(jti, revokedUntil);

            return false;
        }

        return true;
    }

    private void cleanupExpired() {

        Instant now = Instant.now();

        revokedTokens.entrySet()
                .removeIf(entry ->
                        !entry.getValue().isAfter(now)
                );
    }
}