package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.shared.security.JwtService;
import br.com.watchusee.watchusee.user.api.dto.LoginRequest;
import br.com.watchusee.watchusee.user.api.dto.LoginResponse;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.InvalidCredentialsException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String normalizedNick =
                normalizeNick(request.nick());

        User user =
                userRepository
                        .findByNick(normalizedNick)
                        .orElseThrow(() ->
                                new InvalidCredentialsException(
                                        "Nick ou senha inválidos."
                                )
                        );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {

            throw new InvalidCredentialsException(
                    "Nick ou senha inválidos."
            );
        }

        String token =
                jwtService.generateToken(
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

        String normalizedNick =
                nick.trim();

        if (normalizedNick.isBlank()) {

            throw new InvalidCredentialsException(
                    "Nick ou senha inválidos."
            );
        }

        return normalizedNick;
    }
}