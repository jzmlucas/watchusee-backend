package br.com.watchusee.watchusee.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("InMemoryTokenBlacklistService")
class InMemoryTokenBlacklistServiceTest {

    private TokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new InMemoryTokenBlacklistService();
    }

    @Test
    @DisplayName("deve revocar token válido com jti não nulo")
    void shouldRevokeValidTokenWithNonNullJti() {
        Instant expiresAt = Instant.now().plusSeconds(3600);

        assertThatNoException()
                .isThrownBy(() -> blacklistService.revoke("jti-token-123", expiresAt));
    }

    @Test
    @DisplayName("deve revocar token com null expiresAt usando Instant.now()")
    void shouldRevokeTokenWithNullExpiresAt() {
        assertThatNoException()
                .isThrownBy(() -> blacklistService.revoke("jti-token-456", null));
    }

    @Test
    @DisplayName("deve ignorar token com jti em branco")
    void shouldIgnoreTokenWithBlankJti() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        
        assertThatNoException()
                .isThrownBy(() -> blacklistService.revoke("", expiresAt));

        assertThat(blacklistService.isRevoked("")).isFalse();
    }

    @Test
    @DisplayName("deve ignorar token já expirado")
    void shouldIgnoreExpiredToken() {
        Instant expiresAt = Instant.now().minusSeconds(1);
        
        assertThatNoException()
                .isThrownBy(() -> blacklistService.revoke("jti-token-expired", expiresAt));

        assertThat(blacklistService.isRevoked("jti-token-expired")).isFalse();
    }

    @Test
    @DisplayName("deve retornar false para token não revogado")
    void shouldReturnFalseForNotRevokedToken() {
        assertThat(blacklistService.isRevoked("non-existent-token")).isFalse();
    }

    @Test
    @DisplayName("deve retornar true para token revogado")
    void shouldReturnTrueForRevokedToken() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        
        blacklistService.revoke("token-123", expiresAt);
        
        assertThat(blacklistService.isRevoked("token-123")).isTrue();
    }

    @Test
    @DisplayName("deve remover token expirado da lista automaticamente")
    void shouldRemoveExpiredTokenFromListAutomatically() {
        Instant expiresAt = Instant.now().minusSeconds(1);
        
        blacklistService.revoke("expired-token", expiresAt);
        
        assertThat(blacklistService.isRevoked("expired-token")).isFalse();
    }

    @Test
    @DisplayName("deve remover token manualmente ao revocar se estiver expirado")
    void shouldRemoveTokenWhenManuallyRevokingExpired() {
        Instant expiresAt = Instant.now().minusSeconds(1);
        
        blacklistService.revoke("old-token", expiresAt);
        assertThat(blacklistService.isRevoked("old-token")).isFalse();

        Instant newExpiresAt = Instant.now().plusSeconds(3600);
        blacklistService.revoke("old-token", newExpiresAt);
        
        assertThat(blacklistService.isRevoked("old-token")).isTrue();
    }
}
