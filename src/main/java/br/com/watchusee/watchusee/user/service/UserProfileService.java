package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.user.api.dto.UserProfileResponse;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import br.com.watchusee.watchusee.watchlist.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final WatchlistRepository watchlistRepository;

    public UserProfileService(
            UserRepository userRepository,
            WatchlistRepository watchlistRepository
    ) {
        this.userRepository = userRepository;
        this.watchlistRepository = watchlistRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "Usuário não encontrado: " + userId
                                )
                        );

        long watchedMovies =
                watchlistRepository.countByUserIdAndStatus(
                        userId,
                        WatchlistStatus.WATCHED
                );

        long toWatchMovies =
                watchlistRepository.countByUserIdAndStatus(
                        userId,
                        WatchlistStatus.TO_WATCH
                );

        return new UserProfileResponse(
                user.getId(),
                user.getNick(),
                user.getCreatedAt(),
                watchedMovies,
                toWatchMovies
        );
    }
}