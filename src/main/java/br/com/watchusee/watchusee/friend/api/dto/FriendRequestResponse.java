package br.com.watchusee.watchusee.friend.api.dto;

public record FriendRequestResponse(
        Long id,
        Long userId,
        String nick
) {
}