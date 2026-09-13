package br.com.watchusee.watchusee.user.domain;

import br.com.watchusee.watchusee.movie.domain.Movie;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 30
    )
    private String nick;

    @Column(
            name = "password_hash",
            nullable = false
    )
    private String passwordHash;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "avatar_icon",
            length = 30
    )
    private AvatarIcon avatarIcon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "favorite_movie_id",
            foreignKey = @ForeignKey(
                    name = "fk_user_favorite_movie"
            )
    )
    private Movie favoriteMovie;

    @Column(
            name = "failed_login_attempts",
            nullable = false,
            columnDefinition = "integer DEFAULT 0"
    )
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "tokens_valid_after")
    private Instant tokensValidAfter;

    protected User() {
    }

    public User(
            String nick,
            String passwordHash
    ) {
        this.nick = nick;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public User(Long userId, String mail, String nickname, List<Object> of, List<Object> of1, Instant now) {
    }

    public Long getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AvatarIcon getAvatarIcon() {
        return avatarIcon;
    }

    public Movie getFavoriteMovie() {
        return favoriteMovie;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getTokensValidAfter() {
        return tokensValidAfter;
    }

    public void changePassword(String passwordHash) {

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                    "O hash da senha não pode ser nulo ou vazio."
            );
        }

        this.passwordHash = passwordHash;

        this.tokensValidAfter =
                Instant.now()
                        .truncatedTo(ChronoUnit.SECONDS);
    }

    public void updateAvatarIcon(AvatarIcon avatarIcon) {

        if (avatarIcon == null) {
            throw new IllegalArgumentException(
                    "O ícone de avatar não pode ser nulo."
            );
        }

        this.avatarIcon = avatarIcon;
    }

    public void updateFavoriteMovie(Movie favoriteMovie) {

        if (favoriteMovie == null) {
            throw new IllegalArgumentException(
                    "O filme favorito não pode ser nulo."
            );
        }

        this.favoriteMovie = favoriteMovie;
    }

    public void removeFavoriteMovie() {
        this.favoriteMovie = null;
    }

    public boolean isLocked(Instant now) {

        return lockedUntil != null &&
                lockedUntil.isAfter(now);
    }

    public void registerFailedLoginAttempt(
            int maxAttempts,
            java.time.Duration lockDuration,
            Instant now
    ) {

        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = now.plus(lockDuration);
        }
    }

    public void resetFailedLoginAttempts() {

        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void invalidateTokensIssuedBefore(Instant instant) {

        if (instant == null) {
            throw new IllegalArgumentException(
                    "O instante de invalidação não pode ser nulo."
            );
        }

        this.tokensValidAfter =
                instant.truncatedTo(ChronoUnit.SECONDS);
    }

    public boolean isTokenValid(Instant issuedAt) {

        if (issuedAt == null) {
            return false;
        }

        if (tokensValidAfter == null) {
            return true;
        }

        return !issuedAt.isBefore(tokensValidAfter);
    }
}