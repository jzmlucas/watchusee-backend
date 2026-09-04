package br.com.watchusee.watchusee.watchlist.domain;

import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "watchlist",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_watchlist_user_movie",
                        columnNames = {
                                "user_id",
                                "movie_id"
                        }
                )
        }
)
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_watchlist_user"
            )
    )
    private User user;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "movie_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_watchlist_movie"
            )
    )
    private Movie movie;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private WatchlistStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected Watchlist() {
    }

    public Watchlist(
            User user,
            Movie movie,
            WatchlistStatus status
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "O usuário não pode ser nulo."
            );
        }

        if (movie == null) {
            throw new IllegalArgumentException(
                    "O filme não pode ser nulo."
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "O status da watchlist não pode ser nulo."
            );
        }

        this.user = user;
        this.movie = movie;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public void updateStatus(WatchlistStatus status) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "O status da watchlist não pode ser nulo."
            );
        }

        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Movie getMovie() {
        return movie;
    }

    public WatchlistStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}