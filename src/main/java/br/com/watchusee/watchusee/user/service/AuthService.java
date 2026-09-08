package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.shared.security.JwtService;
import br.com.watchusee.watchusee.user.api.dto.LoginRequest;
import br.com.watchusee.watchusee.user.api.dto.LoginResponse;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.AccountLockedException;
import br.com.watchusee.watchusee.user.exception.InvalidCredentialsException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthService.class);

    private static final String DUMMY_PASSWORD_HASH =
            "{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5a2Vc9nvVGKp/pWMBP0e9J9v5PoOe";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${security.login.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.login.lock-duration-minutes:15}")
    private long lockDurationMinutes;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        String normalizedNick = normalizeNick(request.nick());

        User user = userRepository
                .findByNick(normalizedNick)
                .orElse(null);

        if (user == null) {
            passwordEncoder.matches(
                    request.password(),
                    DUMMY_PASSWORD_HASH
            );

            log.warn(
                    "Tentativa de login com nick inexistente: {}",
                    normalizedNick
            );

            throw new InvalidCredentialsException(
                    "Nick ou senha inválidos."
            );
        }

        Instant now = Instant.now();

        if (user.isLocked(now)) {
            log.warn(
                    "Tentativa de login em conta bloqueada. userId={}",
                    user.getId()
            );

            throw new AccountLockedException(
                    "Conta temporariamente bloqueada devido a múltiplas " +
                            "tentativas de login inválidas. Tente novamente mais tarde."
            );
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            user.registerFailedLoginAttempt(
                    maxFailedAttempts,
                    Duration.ofMinutes(lockDurationMinutes),
                    now
            );

            userRepository.save(user);

            log.warn(
                    "Login inválido. userId={} tentativasFalhas={}",
                    user.getId(),
                    user.getFailedLoginAttempts()
            );

            throw new InvalidCredentialsException(
                    "Nick ou senha inválidos."
            );
        }

        user.resetFailedLoginAttempts();

        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getId()
        );

        log.info(
                "Login realizado com sucesso. userId={}",
                user.getId()
        );

        return new LoginResponse(
                user.getId(),
                user.getNick(),
                token
        );
    }

    private String normalizeNick(String nick) {

        if (nick == null) {
            throw new InvalidCredentialsException(
                    "Nick ou senha inválidos."
            );
        }

        String normalizedNick = nick.trim();

        if (normalizedNick.isBlank()) {
            throw new InvalidCredentialsException(
                    "Nick ou senha inválidos."
            );
        }

        return normalizedNick;
    }
}