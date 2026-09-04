package br.com.watchusee.watchusee.share.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateShareRequest(

        @NotNull(
                message = "O movieId é obrigatório."
        )
        @Positive(
                message = "O movieId deve ser positivo."
        )
        Long movieId,

        @NotBlank(
                message = "O nick do destinatário é obrigatório."
        )
        @Size(
                min = 3,
                max = 30,
                message = "O nick deve possuir entre 3 e 30 caracteres."
        )
        String recipientNick,

        @Size(
                max = 500,
                message = "A mensagem deve possuir no máximo 500 caracteres."
        )
        String message

) {
}