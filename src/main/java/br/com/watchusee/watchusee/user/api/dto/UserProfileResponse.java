package br.com.watchusee.watchusee.user.api.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String nick,
        Instant createdAt,
        long watchedMovies,
        long toWatchMovies
) {
}