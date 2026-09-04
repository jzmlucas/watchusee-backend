package br.com.watchusee.watchusee.watchlist.api.dto;

import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateWatchlistRequest(

        @NotNull(message = "O status é obrigatório.")
        WatchlistStatus status

) {
}