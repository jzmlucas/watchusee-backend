package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.friend.domain.FriendshipStatus;
import br.com.watchusee.watchusee.friend.repository.FriendshipRepository;
import br.com.watchusee.watchusee.user.api.dto.FavoriteMovieResponse;
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
    private final FriendshipRepository friendshipRepository;

    public UserProfileService(
            UserRepository userRepository,
            WatchlistRepository watchlistRepository,
            FriendshipRepository friendshipRepository
    ) {
        this.userRepository = userRepository;
        this.watchlistRepository = watchlistRepository;
        this.friendshipRepository = friendshipRepository;
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

        long friendsCount =
                friendshipRepository
                        .countByRequesterIdAndStatus(
                                userId,
                                FriendshipStatus.ACCEPTED
                        )
                        +
                        friendshipRepository
                                .countByReceiverIdAndStatus(
                                        userId,
                                        FriendshipStatus.ACCEPTED
                                );

        FavoriteMovieResponse favoriteMovieResponse =
                user.getFavoriteMovie() == null
                        ? null
                        : new FavoriteMovieResponse(
                        user.getFavoriteMovie().getId(),
                        user.getFavoriteMovie().getTitle(),
                        user.getFavoriteMovie().getPosterPath()
                );

        return new UserProfileResponse(
                user.getId(),
                user.getNick(),
                user.getCreatedAt(),
                watchedMovies,
                toWatchMovies,
                friendsCount,
                user.getAvatarIcon(),
                favoriteMovieResponse
        );
    }
}