package br.com.watchusee.watchusee.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    private static final String VALID_SECRET =
            "01234567890123456789012345678901"; // 33 caracteres

    private static final long EXPIRATION_MS = 60_000L;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                VALID_SECRET,
                EXPIRATION_MS,
                "watchusee-api",
                tokenBlacklistService
        );
    }

    @Test
    @DisplayName("deve gerar um token do qual é possível extrair o mesmo userId")
    void shouldGenerateTokenAndExtractUserId() {

        String token = jwtService.generateToken(42L);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("deve considerar válido um token recém-gerado e não revogado")
    void shouldConsiderFreshTokenValid() {

        String token = jwtService.generateToken(1L);

        when(tokenBlacklistService.isRevoked(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("deve considerar inválido um token revogado (após logout)")
    void shouldConsiderRevokedTokenInvalid() {

        String token = jwtService.generateToken(1L);

        when(tokenBlacklistService.isRevoked(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true);

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("deve considerar inválido um token malformado")
    void shouldConsiderMalformedTokenInvalid() {

        assertThat(jwtService.isValid("token-invalido")).isFalse();
    }

    @Test
    @DisplayName("deve considerar inválido um token assinado com outra chave")
    void shouldConsiderTokenSignedWithDifferentKeyInvalid() {

        JwtService otherService = new JwtService(
                "outra-chave-completamente-diferente-000",
                EXPIRATION_MS,
                "watchusee-api",
                tokenBlacklistService
        );

        String tokenFromOtherService = otherService.generateToken(1L);

        assertThat(jwtService.isValid(tokenFromOtherService)).isFalse();
    }

    @Test
    @DisplayName("deve chamar o blacklist ao revogar um token válido")
    void shouldRevokeValidToken() {

        String token = jwtService.generateToken(1L);

        jwtService.revoke(token);

        org.mockito.Mockito.verify(tokenBlacklistService)
                .revoke(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(Instant.class)
                );
    }

    @Test
    @DisplayName("não deve lançar exceção ao tentar revogar um token inválido")
    void shouldNotThrowWhenRevokingInvalidToken() {

        jwtService.revoke("token-totalmente-invalido");

        org.mockito.Mockito.verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    @DisplayName("deve rejeitar a criação do serviço com uma chave secreta curta demais")
    void shouldRejectShortSecret() {

        assertThatThrownBy(() ->
                new JwtService(
                        "chave-curta",
                        EXPIRATION_MS,
                        "watchusee-api",
                        tokenBlacklistService
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("extractIssuedAt deve retornar o instante de emissão do token")
    void shouldExtractIssuedAt() {

        Instant before = Instant.now().minusSeconds(1);

        String token = jwtService.generateToken(1L);

        Instant issuedAt = jwtService.extractIssuedAt(token);

        assertThat(issuedAt).isAfterOrEqualTo(before);
    }
}
