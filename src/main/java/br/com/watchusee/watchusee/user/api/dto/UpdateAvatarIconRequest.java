package br.com.watchusee.watchusee.user.api.dto;

import br.com.watchusee.watchusee.user.domain.AvatarIcon;
import jakarta.validation.constraints.NotNull;

public record UpdateAvatarIconRequest(

        @NotNull(message = "O ícone de avatar é obrigatório.")
        AvatarIcon icon

) {
}