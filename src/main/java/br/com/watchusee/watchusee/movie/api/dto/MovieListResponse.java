package br.com.watchusee.watchusee.movie.api.dto;

import java.util.List;

public record MovieListResponse(

        int page,

        int totalPages,

        int totalResults,

        List<MovieListItemResponse> results
) {
}