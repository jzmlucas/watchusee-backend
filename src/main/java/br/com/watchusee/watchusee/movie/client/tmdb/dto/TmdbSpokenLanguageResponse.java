package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbSpokenLanguageResponse(
        @JsonProperty("english_name")
        String englishName,

        @JsonProperty("iso_639_1")
        String iso6391,

        String name
) {
}