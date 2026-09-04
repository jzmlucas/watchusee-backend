package br.com.watchusee.watchusee.movie.dto;

public record MovieTrailerResponse(
        String key,
        String name,
        String site,
        String type
) {
}