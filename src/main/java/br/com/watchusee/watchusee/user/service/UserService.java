package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.user.api.dto.CreateUserRequest;
import br.com.watchusee.watchusee.user.api.dto.UserResponse;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.UserAlreadyExistsException;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(
            CreateUserRequest request
    ) {

        String normalizedNick =
                normalizeNick(request.nick());

        if (userRepository.existsByNick(normalizedNick)) {

            throw new UserAlreadyExistsException(
                    "O nick informado já está em uso."
            );
        }

        String passwordHash =
                passwordEncoder.encode(
                        request.password()
                );

        User user =
                new User(
                        normalizedNick,
                        passwordHash
                );

        User savedUser =
                userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getNick()
        );
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {

        if (userId == null || userId <= 0) {

            throw new UserNotFoundException(
                    "Usuário não encontrado."
            );
        }

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Usuário não encontrado: " + userId
                        )
                );
    }

    @Transactional(readOnly = true)
    public User findByNick(String nick) {

        String normalizedNick =
                normalizeNick(nick);

        return userRepository
                .findByNick(normalizedNick)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Usuário não encontrado: "
                                        + normalizedNick
                        )
                );
    }

    private String normalizeNick(String nick) {

        if (nick == null) {

            throw new IllegalArgumentException(
                    "O nick não pode ser nulo."
            );
        }

        String normalizedNick =
                nick.trim();

        if (normalizedNick.isBlank()) {

            throw new IllegalArgumentException(
                    "O nick não pode estar vazio."
            );
        }

        return normalizedNick;
    }
}