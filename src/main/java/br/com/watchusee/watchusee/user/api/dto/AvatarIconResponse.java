package br.com.watchusee.watchusee.user.api.dto;

import br.com.watchusee.watchusee.user.domain.AvatarIcon;

public record AvatarIconResponse(
        AvatarIcon value,
        String label
) {
}