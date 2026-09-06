package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbReviewAuthorDetailsResponse(

        String name,

        String username,

        @JsonProperty("avatar_path")
        String avatarPath,

        Double rating
) {
}