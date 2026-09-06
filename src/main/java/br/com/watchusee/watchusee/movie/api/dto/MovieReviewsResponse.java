package br.com.watchusee.watchusee.movie.api.dto;

import java.util.List;

public record MovieReviewsResponse(

        int page,

        int totalPages,

        int totalResults,

        List<MovieReviewItemResponse> results
) {
}