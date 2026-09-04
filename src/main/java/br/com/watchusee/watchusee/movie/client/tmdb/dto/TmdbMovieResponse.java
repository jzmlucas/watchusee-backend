package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record TmdbMovieResponse(

        Long id,
        String title,
        String overview,

        @JsonProperty("release_date")
        LocalDate releaseDate,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("vote_average")
        Double rating

) {
}