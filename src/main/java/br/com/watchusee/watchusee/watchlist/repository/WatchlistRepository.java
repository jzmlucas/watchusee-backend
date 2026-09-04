package br.com.watchusee.watchusee.watchlist.repository;

import br.com.watchusee.watchusee.watchlist.domain.Watchlist;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WatchlistRepository
        extends JpaRepository<Watchlist, Long> {

    @EntityGraph(attributePaths = "movie")
    Optional<Watchlist> findByUserIdAndMovieId(
            Long userId,
            Long movieId
    );

    @EntityGraph(attributePaths = "movie")
    Page<Watchlist> findAllByUserId(
            Long userId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "movie")
    Page<Watchlist> findAllByUserIdAndStatus(
            Long userId,
            WatchlistStatus status,
            Pageable pageable
    );

    boolean existsByUserIdAndMovieIdAndStatus(
            Long userId,
            Long movieId,
            WatchlistStatus status
    );

    long countByUserIdAndStatus(
            Long userId,
            WatchlistStatus status
    );

    void deleteByUserIdAndMovieId(
            Long userId,
            Long movieId
    );
}