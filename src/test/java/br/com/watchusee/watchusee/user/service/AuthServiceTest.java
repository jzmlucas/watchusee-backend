package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.shared.security.JwtService;
import br.com.watchusee.watchusee.user.api.dto.LoginRequest;
import br.com.watchusee.watchusee.user.api.dto.LoginResponse;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.AccountLockedException;
import br.com.watchusee.watchusee.user.exception.InvalidCredentialsException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private static final String DUMMY_PASSWORD_HASH =
            "{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5a2Vc9nvVGKp/pWMBP0e9J9v5PoOe";

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService
        );

        ReflectionTestUtils.setField(
                authService,
                "maxFailedAttempts",
                MAX_ATTEMPTS
        );

        ReflectionTestUtils.setField(
                authService,
                "lockDurationMinutes",
                LOCK_DURATION_MINUTES
        );
    }

    private User buildUser(
            Long id,
            String nick,
            String passwordHash
    ) {
        User user = new User(nick, passwordHash);

        ReflectionTestUtils.setField(
                user,
                "id",
                id
        );

        return user;
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("deve retornar token quando nick e senha são válidos")
        void shouldReturnTokenWhenCredentialsAreValid() {
            User user = buildUser(
                    1L,
                    "lucas",
                    "hashed-password"
            );

            when(userRepository.findByNick("lucas"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(
                    "123456",
                    "hashed-password"
            )).thenReturn(true);

            when(jwtService.generateToken(1L))
                    .thenReturn("jwt-token");

            LoginResponse response = authService.login(
                    new LoginRequest(
                            "lucas",
                            "123456"
                    )
            );

            assertThat(response.id())
                    .isEqualTo(1L);

            assertThat(response.nick())
                    .isEqualTo("lucas");

            assertThat(response.token())
                    .isEqualTo("jwt-token");

            assertThat(user.getFailedLoginAttempts())
                    .isZero();

            assertThat(user.getLockedUntil())
                    .isNull();

            verify(userRepository)
                    .save(user);

            verify(jwtService)
                    .generateToken(1L);
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando o nick não existe")
        void shouldThrowWhenNickDoesNotExist() {
            when(userRepository.findByNick("desconhecido"))
                    .thenReturn(Optional.empty());

            when(passwordEncoder.matches(
                    "qualquer123",
                    DUMMY_PASSWORD_HASH
            )).thenReturn(false);

            assertThatThrownBy(() ->
                    authService.login(
                            new LoginRequest(
                                    "desconhecido",
                                    "qualquer123"
                            )
                    )
            ).isInstanceOf(InvalidCredentialsException.class);

            verify(passwordEncoder)
                    .matches(
                            eq("qualquer123"),
                            eq(DUMMY_PASSWORD_HASH)
                    );

            verify(userRepository, never())
                    .save(any());

            verify(jwtService, never())
                    .generateToken(any());
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando a senha está incorreta")
        void shouldThrowWhenPasswordIsIncorrect() {
            User user = buildUser(
                    1L,
                    "lucas",
                    "hashed-password"
            );

            when(userRepository.findByNick("lucas"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(
                    "senhaErrada",
                    "hashed-password"
            )).thenReturn(false);

            assertThatThrownBy(() ->
                    authService.login(
                            new LoginRequest(
                                    "lucas",
                                    "senhaErrada"
                            )
                    )
            ).isInstanceOf(InvalidCredentialsException.class);

            assertThat(user.getFailedLoginAttempts())
                    .isEqualTo(1);

            assertThat(user.getLockedUntil())
                    .isNull();

            verify(userRepository)
                    .save(user);

            verify(jwtService, never())
                    .generateToken(any());
        }

        @Test
        @DisplayName("deve bloquear a conta após atingir o número máximo de tentativas")
        void shouldLockAccountAfterMaxFailedAttempts() {
            User user = buildUser(
                    1L,
                    "lucas",
                    "hashed-password"
            );

            ReflectionTestUtils.setField(
                    user,
                    "failedLoginAttempts",
                    MAX_ATTEMPTS - 1
            );

            when(userRepository.findByNick("lucas"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(
                    "senhaErrada",
                    "hashed-password"
            )).thenReturn(false);

            assertThatThrownBy(() ->
                    authService.login(
                            new LoginRequest(
                                    "lucas",
                                    "senhaErrada"
                            )
                    )
            ).isInstanceOf(InvalidCredentialsException.class);

            assertThat(user.getFailedLoginAttempts())
                    .isEqualTo(MAX_ATTEMPTS);

            assertThat(user.getLockedUntil())
                    .isNotNull();

            assertThat(user.isLocked(Instant.now()))
                    .isTrue();

            verify(userRepository)
                    .save(user);

            verify(jwtService, never())
                    .generateToken(any());
        }

        @Test
        @DisplayName("deve lançar AccountLockedException quando a conta já está bloqueada")
        void shouldThrowWhenAccountIsLocked() {
            User user = buildUser(
                    1L,
                    "lucas",
                    "hashed-password"
            );

            ReflectionTestUtils.setField(
                    user,
                    "failedLoginAttempts",
                    MAX_ATTEMPTS - 1
            );

            Instant now = Instant.now();

            user.registerFailedLoginAttempt(
                    MAX_ATTEMPTS,
                    Duration.ofMinutes(LOCK_DURATION_MINUTES),
                    now
            );

            assertThat(user.getFailedLoginAttempts())
                    .isEqualTo(MAX_ATTEMPTS);

            assertThat(user.getLockedUntil())
                    .isNotNull();

            assertThat(user.isLocked(Instant.now()))
                    .isTrue();

            when(userRepository.findByNick("lucas"))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    authService.login(
                            new LoginRequest(
                                    "lucas",
                                    "123456"
                            )
                    )
            ).isInstanceOf(AccountLockedException.class);

            verify(passwordEncoder, never())
                    .matches(
                            anyString(),
                            anyString()
                    );

            verify(jwtService, never())
                    .generateToken(any());

            verify(userRepository, never())
                    .save(any());
        }

        @Test
        @DisplayName("deve resetar as tentativas falhas após um login bem-sucedido")
        void shouldResetFailedAttemptsAfterSuccessfulLogin() {
            User user = buildUser(
                    1L,
                    "lucas",
                    "hashed-password"
            );

            ReflectionTestUtils.setField(
                    user,
                    "failedLoginAttempts",
                    3
            );

            when(userRepository.findByNick("lucas"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(
                    "senha123",
                    "hashed-password"
            )).thenReturn(true);

            when(jwtService.generateToken(1L))
                    .thenReturn("jwt-token");

            LoginResponse response = authService.login(
                    new LoginRequest(
                            "lucas",
                            "senha123"
                    )
            );

            assertThat(response.token())
                    .isEqualTo("jwt-token");

            assertThat(user.getFailedLoginAttempts())
                    .isZero();

            assertThat(user.getLockedUntil())
                    .isNull();

            verify(userRepository)
                    .save(user);

            verify(jwtService)
                    .generateToken(1L);
        }

        @Test
        @DisplayName("deve rejeitar nick em branco antes de consultar o repositório")
        void shouldRejectBlankNick() {
            assertThatThrownBy(() ->
                    authService.login(
                            new LoginRequest(
                                    "   ",
                                    "senha123"
                            )
                    )
            ).isInstanceOf(InvalidCredentialsException.class);

            verify(userRepository, never())
                    .findByNick(anyString());

            verify(passwordEncoder, never())
                    .matches(
                            anyString(),
                            anyString()
                    );

            verify(jwtService, never())
                    .generateToken(any());
        }
    }
}