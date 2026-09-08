package br.com.watchusee.watchusee.movie.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record TmdbMovieResponse(

        Long id,

        String title,

        @JsonProperty("original_title")
        String originalTitle,

        String overview,

        String tagline,

        @JsonProperty("release_date")
        LocalDate releaseDate,

        Integer runtime,

        @JsonProperty("original_language")
        String originalLanguage,

        @JsonProperty("spoken_languages")
        List<TmdbSpokenLanguageResponse> spokenLanguages,

        List<TmdbGenreResponse> genres,

        @JsonProperty("vote_average")
        Double rating,

        @JsonProperty("vote_count")
        Long voteCount,

        Double popularity,

        String status,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("backdrop_path")
        String backdropPath,

        String homepage,

        Boolean adult,

        Boolean video,

        Long budget,

        Long revenue,

        @JsonProperty("production_companies")
        List<TmdbProductionCompanyResponse> productionCompanies,

        @JsonProperty("production_countries")
        List<TmdbProductionCountryResponse> productionCountries

) {
}