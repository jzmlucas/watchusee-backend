package br.com.watchusee.watchusee.movie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(
        name = "MovieResponse",
        description = "Representação pública de um filme."
)
public record MovieResponse(

        @Schema(
                description = "ID do filme no TMDB.",
                example = "414906"
        )
        Long id,

        @Schema(
                description = "Título do filme.",
                example = "The Batman"
        )
        String title,

        @Schema(
                description = "Sinopse do filme.",
                example = "In his second year of fighting crime, Batman uncovers corruption in Gotham City."
        )
        String overview,

        @Schema(
                description = "Data de lançamento.",
                example = "2022-03-01"
        )
        LocalDate releaseDate,

        @Schema(
                description = "Caminho relativo para o pôster do filme no TMDB.",
                example = "/74xTEgt7R36Fpooo50r9T25onhq.jpg"
        )
        String posterPath,

        @Schema(
                description = "Nota média atribuída pelos usuários do TMDB.",
                example = "7.668",
                minimum = "0",
                maximum = "10"
        )
        Double rating
) {
}