package br.com.watchusee.watchusee.share.api.dto;

import br.com.watchusee.watchusee.share.domain.ShareStatus;

import java.time.Instant;

public record ShareResponse(

        Long id,

        Long movieId,

        Long senderId,

        String senderNick,

        Long recipientId,

        String recipientNick,

        String message,

        ShareStatus status,

        Instant createdAt

) {
}