package br.com.watchusee.watchusee.movie.api.dto;

import java.util.List;

public record MoviePageResponse(

        int page,

        int totalPages,

        int totalResults,

        List<MovieResponse> results
) {
}