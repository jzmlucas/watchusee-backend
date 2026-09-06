package br.com.watchusee.watchusee.user.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateFavoriteMovieRequest(

        @NotNull(message = "O ID do filme é obrigatório.")
        @Positive(message = "O ID do filme deve ser maior que zero.")
        Long movieId

) {
}