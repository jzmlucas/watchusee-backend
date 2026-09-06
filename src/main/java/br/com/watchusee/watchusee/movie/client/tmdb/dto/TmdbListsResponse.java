package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbListsResponse(

        int id,

        int page,

        List<TmdbListResponse> results,

        @JsonProperty("total_pages")
        int totalPages,

        @JsonProperty("total_results")
        int totalResults
) {
}