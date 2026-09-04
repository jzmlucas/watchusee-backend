package br.com.watchusee.watchusee.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

@NotBlank(message = "O nick é obrigatório.")
@Size(
        min = 3,
        max = 30,
        message = "O nick deve possuir entre 3 e 30 caracteres."
)
String nick,

@NotBlank(message = "A senha é obrigatória.")
@Size(
        min = 6,
        max = 100,
        message = "A senha deve possuir entre 6 e 100 caracteres."
)
String password

) {
}
