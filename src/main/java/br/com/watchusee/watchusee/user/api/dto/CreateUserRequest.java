package br.com.watchusee.watchusee.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "O nick é obrigatório.")
        @Size(
                min = 3,
                max = 30,
                message = "O nick deve possuir entre 3 e 30 caracteres."
        )
        @Pattern(
                regexp = "^[a-zA-Z0-9_.]+$",
                message = "O nick deve conter apenas letras, números, ponto e underscore."
        )
        String nick,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(
                min = 6,
                max = 100,
                message = "A senha deve possuir entre 6 e 100 caracteres."
        )
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "A senha deve conter ao menos uma letra e um número."
        )
        String password
) {
}