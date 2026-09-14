package br.com.watchusee.watchusee.user.api.dto;

import br.com.watchusee.watchusee.user.domain.AvatarIcon;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String nick,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt,
        long watchedMovies,
        long toWatchMovies,
        long friendsCount,
        AvatarIcon avatarIcon,
        FavoriteMovieResponse favoriteMovie
) {
}