package br.com.watchusee.watchusee.friend.api.dto;

public record FriendshipStatusResponse(
        Long userId,
        FriendRelationStatus status,
        Long friendshipId
) {
}
