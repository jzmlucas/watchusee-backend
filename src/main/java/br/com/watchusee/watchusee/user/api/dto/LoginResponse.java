package br.com.watchusee.watchusee.user.api.dto;

public record LoginResponse(
        Long id,
        String nick,
        String token
) {
}