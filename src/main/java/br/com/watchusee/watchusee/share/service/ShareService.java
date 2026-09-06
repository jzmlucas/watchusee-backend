package br.com.watchusee.watchusee.share.service;

import br.com.watchusee.watchusee.share.api.dto.CreateShareRequest;
import br.com.watchusee.watchusee.share.api.dto.ShareResponse;
import br.com.watchusee.watchusee.share.domain.Share;
import br.com.watchusee.watchusee.share.domain.ShareStatus;
import br.com.watchusee.watchusee.share.exception.ShareRecipientNotFoundException;
import br.com.watchusee.watchusee.share.repository.ShareRepository;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShareService {

    private final ShareRepository shareRepository;
    private final UserRepository userRepository;

    public ShareService(
            ShareRepository shareRepository,
            UserRepository userRepository
    ) {
        this.shareRepository = shareRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ShareResponse createShare(
            Long senderId,
            CreateShareRequest request
    ) {

        User sender =
                userRepository.findById(senderId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Usuário autenticado não encontrado."
                                )
                        );

        String recipientNick =
                normalizeNick(request.recipientNick());

        User recipient =
                userRepository.findByNick(recipientNick)
                        .orElseThrow(() ->
                                new ShareRecipientNotFoundException(
                                        "Usuário destinatário não encontrado."
                                )
                        );

        if (sender.getId().equals(recipient.getId())) {

            throw new IllegalArgumentException(
                    "Você não pode compartilhar um filme com você mesmo."
            );
        }

        boolean alreadyShared =
                shareRepository
                        .existsBySenderIdAndRecipientIdAndMovieIdAndStatus(
                                sender.getId(),
                                recipient.getId(),
                                request.movieId(),
                                ShareStatus.PENDING
                        );

        if (alreadyShared) {

            throw new IllegalArgumentException(
                    "Este filme já foi compartilhado com esse usuário."
            );
        }

        Share share =
                new Share(
                        sender,
                        recipient,
                        request.movieId(),
                        normalizeMessage(request.message())
                );

        Share savedShare =
                shareRepository.save(share);

        return toResponse(savedShare);
    }

    @Transactional(readOnly = true)
    public List<ShareResponse> getReceivedShares(
            Long userId
    ) {

        return shareRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShareResponse> getPendingShares(
            Long userId
    ) {

        return shareRepository
                .findByRecipientIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        ShareStatus.PENDING
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShareResponse> getSentShares(
            Long userId
    ) {

        return shareRepository
                .findBySenderIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ShareResponse acceptShare(
            Long userId,
            Long shareId
    ) {

        Share share =
                shareRepository
                        .findByIdAndRecipientId(
                                shareId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Compartilhamento não encontrado."
                                )
                        );

        if (share.getStatus() != ShareStatus.PENDING) {

            throw new IllegalStateException(
                    "Este compartilhamento não está pendente."
            );
        }

        share.accept();

        return toResponse(share);
    }

    @Transactional
    public ShareResponse rejectShare(
            Long userId,
            Long shareId
    ) {

        Share share =
                shareRepository
                        .findByIdAndRecipientId(
                                shareId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Compartilhamento não encontrado."
                                )
                        );

        if (share.getStatus() != ShareStatus.PENDING) {

            throw new IllegalStateException(
                    "Este compartilhamento não está pendente."
            );
        }

        share.reject();

        return toResponse(share);
    }

    private ShareResponse toResponse(
            Share share
    ) {

        return new ShareResponse(
                share.getId(),

                share.getMovieId(),

                share.getSender().getId(),
                share.getSender().getNick(),

                share.getRecipient().getId(),
                share.getRecipient().getNick(),

                share.getMessage(),

                share.getStatus(),

                share.getCreatedAt()
        );
    }

    private String normalizeNick(
            String nick
    ) {

        if (nick == null) {

            throw new IllegalArgumentException(
                    "O nick do destinatário não pode ser nulo."
            );
        }

        String normalized =
                nick.trim();

        if (normalized.isBlank()) {

            throw new IllegalArgumentException(
                    "O nick do destinatário não pode estar vazio."
            );
        }

        return normalized;
    }

    private String normalizeMessage(
            String message
    ) {

        if (message == null) {
            return null;
        }

        String normalized =
                message.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }
}