package br.com.watchusee.watchusee.movie.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MovieSearchRequest(

        @NotBlank(message = "O termo de busca é obrigatório.")
        @Size(
                min = 2,
                max = 100,
                message = "O termo de busca deve possuir entre 2 e 100 caracteres."
        )
        String query

) {
}