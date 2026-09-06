package br.com.watchusee.watchusee.user.api.dto;

public record FavoriteMovieResponse(
        Long id,
        String title,
        String posterPath
) {
}