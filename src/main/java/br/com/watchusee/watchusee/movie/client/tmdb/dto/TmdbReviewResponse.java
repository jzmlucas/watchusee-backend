package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbReviewResponse(

        String id,

        String author,

        @JsonProperty("author_details")
        TmdbReviewAuthorDetailsResponse authorDetails,

        String content,

        @JsonProperty("created_at")
        String createdAt,

        @JsonProperty("updated_at")
        String updatedAt,

        String url
) {
}