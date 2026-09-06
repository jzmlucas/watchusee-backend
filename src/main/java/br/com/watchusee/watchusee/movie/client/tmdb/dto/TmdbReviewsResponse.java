package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbReviewsResponse(

        int id,

        int page,

        List<TmdbReviewResponse> results,

        @JsonProperty("total_pages")
        int totalPages,

        @JsonProperty("total_results")
        int totalResults
) {
}