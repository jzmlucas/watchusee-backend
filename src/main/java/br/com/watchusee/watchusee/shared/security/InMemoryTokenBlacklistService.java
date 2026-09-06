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

        revokedTokens.put(jti, expiresAt != null ? expiresAt : Instant.now());
        cleanupExpired();
    }

    @Override
    public boolean isRevoked(String jti) {

        if (jti == null) {
            return false;
        }

        return revokedTokens.containsKey(jti);
    }

    private void cleanupExpired() {

        Instant now = Instant.now();

        revokedTokens.entrySet()
                .removeIf(entry -> entry.getValue().isBefore(now));
    }
}
