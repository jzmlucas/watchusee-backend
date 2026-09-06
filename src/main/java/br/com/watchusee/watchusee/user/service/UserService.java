package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.movie.repository.MovieRepository;
import br.com.watchusee.watchusee.movie.service.MovieService;
import br.com.watchusee.watchusee.user.api.dto.AvatarIconResponse;
import br.com.watchusee.watchusee.user.api.dto.ChangePasswordRequest;
import br.com.watchusee.watchusee.user.api.dto.CreateUserRequest;
import br.com.watchusee.watchusee.user.api.dto.UserResponse;
import br.com.watchusee.watchusee.user.api.dto.UserSearchResponse;
import br.com.watchusee.watchusee.user.domain.AvatarIcon;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.InvalidCredentialsException;
import br.com.watchusee.watchusee.user.exception.UserAlreadyExistsException;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MovieRepository movieRepository;
    private final MovieService movieService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MovieRepository movieRepository,
            MovieService movieService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.movieRepository = movieRepository;
        this.movieService = movieService;
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

    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {

        User user =
                findById(userId);

        boolean currentPasswordMatches =
                passwordEncoder.matches(
                        request.currentPassword(),
                        user.getPasswordHash()
                );

        if (!currentPasswordMatches) {

            throw new InvalidCredentialsException(
                    "A senha atual está incorreta."
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {

            throw new IllegalArgumentException(
                    "A nova senha deve ser diferente da senha atual."
            );
        }

        String newPasswordHash =
                passwordEncoder.encode(
                        request.newPassword()
                );

        user.changePassword(newPasswordHash);

        user.invalidateTokensIssuedBefore(Instant.now());

        userRepository.save(user);
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

    public List<UserSearchResponse> searchUsers(
            Long authenticatedUserId,
            String query
    ) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String normalizedQuery = query.trim();

        return userRepository
                .findTop20ByNickContainingIgnoreCaseOrderByNickAsc(
                        normalizedQuery
                )
                .stream()
                .filter(user -> !user.getId().equals(authenticatedUserId))
                .map(user -> new UserSearchResponse(
                        user.getId(),
                        user.getNick()
                ))
                .toList();
    }

    public List<AvatarIconResponse> listAvatarIcons() {

        return Arrays.stream(AvatarIcon.values())
                .map(icon -> new AvatarIconResponse(
                        icon,
                        formatLabel(icon)
                ))
                .toList();
    }

    @Transactional
    public void updateAvatarIcon(
            Long userId,
            AvatarIcon icon
    ) {

        User user =
                findById(userId);

        user.updateAvatarIcon(icon);

        userRepository.save(user);
    }

    @Transactional
    public void updateFavoriteMovie(
            Long userId,
            Long movieId
    ) {

        User user =
                findById(userId);

        Movie movie =
                findOrCreateMovie(movieId);

        user.updateFavoriteMovie(movie);

        userRepository.save(user);
    }

    @Transactional
    public void removeFavoriteMovie(Long userId) {

        User user =
                findById(userId);

        user.removeFavoriteMovie();

        userRepository.save(user);
    }

    private Movie findOrCreateMovie(Long movieId) {

        if (movieId == null || movieId <= 0) {
            throw new IllegalArgumentException(
                    "O ID do filme deve ser maior que zero."
            );
        }

        return movieRepository
                .findById(movieId)
                .orElseGet(() -> {

                    Movie movie =
                            movieService.getMovie(movieId);

                    return movieRepository.save(movie);
                });
    }

    private String formatLabel(AvatarIcon icon) {

        String[] words =
                icon.name().split("_");

        StringBuilder label =
                new StringBuilder();

        for (String word : words) {

            if (!label.isEmpty()) {
                label.append(" ");
            }

            label.append(word.charAt(0));
            label.append(word.substring(1).toLowerCase());
        }

        return label.toString();
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