package br.com.watchusee.watchusee.friend.api.dto;

import java.time.Instant;

public record FriendResponse(
        Long id,
        String nick,
        Instant friendsSince
) {
}
