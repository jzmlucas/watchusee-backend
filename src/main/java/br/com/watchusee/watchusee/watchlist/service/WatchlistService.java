package br.com.watchusee.watchusee.watchlist.service;

import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.movie.repository.MovieRepository;
import br.com.watchusee.watchusee.movie.service.MovieService;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import br.com.watchusee.watchusee.watchlist.domain.Watchlist;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import br.com.watchusee.watchusee.watchlist.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final MovieService movieService;

    public WatchlistService(
            WatchlistRepository watchlistRepository,
            UserRepository userRepository,
            MovieRepository movieRepository,
            MovieService movieService
    ) {
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.movieService = movieService;
    }

    @Transactional
    public void addToWatch(
            Long userId,
            Long movieId
    ) {

        User user = findUser(userId);

        Movie movie = findOrCreateMovie(movieId);

        Watchlist watchlist =
                watchlistRepository
                        .findByUserIdAndMovieId(userId, movieId)
                        .orElse(null);

        if (watchlist != null) {

            if (watchlist.getStatus() == WatchlistStatus.TO_WATCH) {
                return;
            }

            watchlistRepository.deleteByUserIdAndMovieId(
                    userId,
                    movieId
            );
        }

        Watchlist newWatchlist = new Watchlist(
                user,
                movie,
                WatchlistStatus.TO_WATCH
        );

        watchlistRepository.save(newWatchlist);
    }

    @Transactional
    public void removeFromWatch(
            Long userId,
            Long movieId
    ) {

        findUser(userId);

        watchlistRepository
                .findByUserIdAndMovieId(userId, movieId)
                .filter(watchlist ->
                        watchlist.getStatus() == WatchlistStatus.TO_WATCH
                )
                .ifPresent(watchlist ->
                        watchlistRepository.deleteByUserIdAndMovieId(
                                userId,
                                movieId
                        )
                );
    }

    @Transactional
    public void markAsWatched(
            Long userId,
            Long movieId
    ) {

        User user = findUser(userId);

        Movie movie = findOrCreateMovie(movieId);

        Watchlist watchlist =
                watchlistRepository
                        .findByUserIdAndMovieId(userId, movieId)
                        .orElse(null);

        if (watchlist != null) {

            if (watchlist.getStatus() == WatchlistStatus.WATCHED) {
                return;
            }

            watchlistRepository.deleteByUserIdAndMovieId(
                    userId,
                    movieId
            );
        }

        Watchlist newWatchlist = new Watchlist(
                user,
                movie,
                WatchlistStatus.WATCHED
        );

        watchlistRepository.save(newWatchlist);
    }

    @Transactional
    public void removeFromWatched(
            Long userId,
            Long movieId
    ) {

        findUser(userId);

        watchlistRepository
                .findByUserIdAndMovieId(userId, movieId)
                .filter(watchlist ->
                        watchlist.getStatus() == WatchlistStatus.WATCHED
                )
                .ifPresent(watchlist ->
                        watchlistRepository.deleteByUserIdAndMovieId(
                                userId,
                                movieId
                        )
                );
    }

    @Transactional(readOnly = true)
    public boolean isToWatch(
            Long userId,
            Long movieId
    ) {

        findUser(userId);

        return watchlistRepository
                .existsByUserIdAndMovieIdAndStatus(
                        userId,
                        movieId,
                        WatchlistStatus.TO_WATCH
                );
    }

    @Transactional(readOnly = true)
    public boolean isWatched(
            Long userId,
            Long movieId
    ) {

        findUser(userId);

        return watchlistRepository
                .existsByUserIdAndMovieIdAndStatus(
                        userId,
                        movieId,
                        WatchlistStatus.WATCHED
                );
    }

    @Transactional(readOnly = true)
    public List<Movie> getToWatch(Long userId) {

        findUser(userId);

        return watchlistRepository
                .findAllByUserIdAndStatus(
                        userId,
                        WatchlistStatus.TO_WATCH
                )
                .stream()
                .map(Watchlist::getMovie)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Movie> getWatched(Long userId) {

        findUser(userId);

        return watchlistRepository
                .findAllByUserIdAndStatus(
                        userId,
                        WatchlistStatus.WATCHED
                )
                .stream()
                .map(Watchlist::getMovie)
                .toList();
    }

    @Transactional(readOnly = true)
    public WatchlistStatus getStatus(
            Long userId,
            Long movieId
    ) {

        findUser(userId);

        return watchlistRepository
                .findByUserIdAndMovieId(
                        userId,
                        movieId
                )
                .map(Watchlist::getStatus)
                .orElse(null);
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

                    Movie movie = movieService.getMovie(movieId);

                    return movieRepository.save(movie);
                });
    }

}
