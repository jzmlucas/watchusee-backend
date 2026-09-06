package br.com.watchusee.watchusee.movie.dto;

import br.com.watchusee.watchusee.movie.domain.Movie;

import java.util.List;

public record MoviePageResult(
        int page,
        int totalPages,
        int totalResults,
        List<Movie> results
) {
}
