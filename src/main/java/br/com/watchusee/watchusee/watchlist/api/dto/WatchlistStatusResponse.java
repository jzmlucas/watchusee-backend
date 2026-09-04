package br.com.watchusee.watchusee.watchlist.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "WatchlistStatusResponse",
        description = "Status de um filme dentro da watchlist."
)
public record WatchlistStatusResponse(

        @Schema(
                description = "ID do filme no TMDB.",
                example = "414906"
        )
        Long movieId,

        @Schema(
                description = "Indica se o filme está na lista para assistir.",
                example = "false"
        )
        boolean toWatch,

        @Schema(
                description = "Indica se o filme já foi assistido.",
                example = "true"
        )
        boolean watched
) {
}