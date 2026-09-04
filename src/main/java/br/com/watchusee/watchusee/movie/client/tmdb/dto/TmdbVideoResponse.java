package br.com.watchusee.watchusee.movie.client.tmdb.dto;

public record TmdbVideoResponse(
        String id,
        String key,
        String name,
        String site,
        String type,
        boolean official,
        String publishedAt
) {
}