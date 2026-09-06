package br.com.watchusee.watchusee.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "A senha atual é obrigatória.")
        @Size(
                min = 6,
                max = 100,
                message = "A senha atual deve possuir entre 6 e 100 caracteres."
        )
        String currentPassword,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(
                min = 8,
                max = 100,
                message = "A nova senha deve possuir entre 8 e 100 caracteres."
        )
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "A nova senha deve conter ao menos uma letra e um número."
        )
        String newPassword
) {
}
