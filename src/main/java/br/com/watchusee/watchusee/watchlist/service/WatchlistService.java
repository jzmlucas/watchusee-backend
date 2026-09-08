package br.com.watchusee.watchusee.watchlist.service;

import br.com.watchusee.watchusee.friend.repository.FriendshipRepository;
import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.movie.repository.MovieRepository;
import br.com.watchusee.watchusee.movie.service.MovieService;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import br.com.watchusee.watchusee.watchlist.domain.Watchlist;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import br.com.watchusee.watchusee.watchlist.repository.WatchlistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final MovieService movieService;
    private final FriendshipRepository friendshipRepository;

    public WatchlistService(
            WatchlistRepository watchlistRepository,
            UserRepository userRepository,
            MovieRepository movieRepository,
            MovieService movieService,
            FriendshipRepository friendshipRepository
    ) {
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.movieService = movieService;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public Watchlist updateStatus(
            Long userId,
            Long movieId,
            WatchlistStatus status
    ) {

        User user = findUser(userId);

        Movie movie = findOrCreateMovie(movieId);

        Watchlist watchlist =
                watchlistRepository
                        .findByUserIdAndMovieId(
                                userId,
                                movieId
                        )
                        .orElse(null);

        if (watchlist == null) {

            Watchlist newWatchlist =
                    new Watchlist(
                            user,
                            movie,
                            status
                    );

            return watchlistRepository.save(
                    newWatchlist
            );
        }

        watchlist.updateStatus(status);

        return watchlistRepository.save(watchlist);
    }

    @Transactional
    public void remove(
            Long userId,
            Long movieId
    ) {

        findUser(userId);

        watchlistRepository.deleteByUserIdAndMovieId(
                userId,
                movieId
        );
    }

    @Transactional(readOnly = true)
    public Watchlist get(
            Long userId,
            Long movieId
    ) {

        findUser(userId);

        return watchlistRepository
                .findByUserIdAndMovieId(
                        userId,
                        movieId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Filme não encontrado na watchlist."
                        )
                );
    }

    @Transactional(readOnly = true)
    public Page<Watchlist> findAll(
            Long userId,
            WatchlistStatus status,
            Pageable pageable
    ) {

        findUser(userId);

        if (status == null) {

            return watchlistRepository.findAllByUserId(
                    userId,
                    pageable
            );
        }

        return watchlistRepository.findAllByUserIdAndStatus(
                userId,
                status,
                pageable
        );
    }

    /**
     * Lista a watchlist de um usuário a partir da perspectiva de outro
     * usuário (visualização de perfil de terceiros).
     *
     * Só é permitido quando:
     *   - o visualizador é o próprio dono da watchlist; ou
     *   - o visualizador e o dono são amigos (amizade ACEITA).
     *
     * Isso impede que qualquer usuário autenticado veja a biblioteca
     * privada de outro usuário apenas manipulando o ID na URL (IDOR).
     */
    @Transactional(readOnly = true)
    public Page<Watchlist> findAllForViewer(
            Long viewerId,
            Long targetUserId,
            WatchlistStatus status,
            Pageable pageable
    ) {

        findUser(targetUserId);

        boolean isSelf =
                viewerId.equals(targetUserId);

        boolean areFriends =
                !isSelf &&
                        friendshipRepository
                                .existsAcceptedFriendshipBetween(
                                        viewerId,
                                        targetUserId
                                );

        if (!isSelf && !areFriends) {

            throw new AccessDeniedException(
                    "Você só pode visualizar a watchlist de amigos."
            );
        }

        return findAll(
                targetUserId,
                status,
                pageable
        );
    }

    private User findUser(Long userId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "O ID do usuário deve ser maior que zero."
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
}