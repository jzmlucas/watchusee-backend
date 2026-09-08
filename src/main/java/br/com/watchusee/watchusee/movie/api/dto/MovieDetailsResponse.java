package br.com.watchusee.watchusee.movie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "MovieDetailsResponse",
        description = "Informações completas de um filme."
)
public record MovieDetailsResponse(

        @Schema(example = "414906")
        Long id,

        @Schema(example = "The Batman")
        String title,

        @Schema(example = "The Batman")
        String originalTitle,

        @Schema(example = "In his second year of fighting crime...")
        String overview,

        @Schema(example = "Unmask the truth.")
        String tagline,

        @Schema(example = "2022-03-01")
        LocalDate releaseDate,

        @Schema(example = "176")
        Integer runtime,

        @Schema(example = "2022")
        Integer releaseYear,

        @Schema(example = "en")
        String originalLanguage,

        List<LanguageResponse> spokenLanguages,

        List<GenreResponse> genres,

        @Schema(example = "7.668")
        Double rating,

        @Schema(example = "8500")
        Long voteCount,

        @Schema(example = "123.45")
        Double popularity,

        @Schema(example = "Released")
        String status,

        String posterPath,

        String backdropPath,

        String homepage,

        Boolean adult,

        Long budget,

        Long revenue,

        List<ProductionCompanyResponse> productionCompanies,

        List<ProductionCountryResponse> productionCountries
) {
}