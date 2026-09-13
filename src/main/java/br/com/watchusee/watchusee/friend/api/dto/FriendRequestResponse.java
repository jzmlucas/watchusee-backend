package br.com.watchusee.watchusee.friend.api.dto;

import java.time.Instant;

public record FriendRequestResponse(
        Long id,
        Long userId,
        String nick,
        Instant requestedAt
) {
}
