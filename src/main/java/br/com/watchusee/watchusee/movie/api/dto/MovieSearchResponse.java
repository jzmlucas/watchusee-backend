package br.com.watchusee.watchusee.movie.api.dto;

import java.util.List;

public record MovieSearchResponse(
        List<MovieResponse> content,
        int page,
        int totalPages,
        int totalResults
) {
}