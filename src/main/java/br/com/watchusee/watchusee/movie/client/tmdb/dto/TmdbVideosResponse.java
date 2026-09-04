package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import java.util.List;

public record TmdbVideosResponse(
        int id,
        List<TmdbVideoResponse> results
) {
}