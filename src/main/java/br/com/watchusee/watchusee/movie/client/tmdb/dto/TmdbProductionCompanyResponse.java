package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbProductionCompanyResponse(
        Long id,
        String name,

        @JsonProperty("logo_path")
        String logoPath,

        @JsonProperty("origin_country")
        String originCountry
) {
}