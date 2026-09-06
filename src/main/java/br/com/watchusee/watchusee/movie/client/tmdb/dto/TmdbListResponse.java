package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbListResponse(

        int id,

        String name,

        String description,

        @JsonProperty("item_count")
        int itemCount,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("backdrop_path")
        String backdropPath
) {
}