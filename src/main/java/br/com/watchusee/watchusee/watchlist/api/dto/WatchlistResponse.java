package br.com.watchusee.watchusee.watchlist.api.dto;

import br.com.watchusee.watchusee.movie.api.dto.MovieResponse;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        name = "WatchlistResponse",
        description = "Representação de um filme na watchlist do usuário."
)
public record WatchlistResponse(

        @Schema(
                description = "Dados do filme."
        )
        MovieResponse movie,

        @Schema(
                description = "Status atual do filme na watchlist.",
                example = "TO_WATCH"
        )
        WatchlistStatus status,

        @Schema(
                description = "Data em que o filme foi adicionado à watchlist."
        )
        Instant createdAt

) {
}