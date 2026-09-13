package br.com.watchusee.watchusee.friend.service;

import br.com.watchusee.watchusee.friend.api.dto.FriendRelationStatus;
import br.com.watchusee.watchusee.friend.api.dto.FriendRequestResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendshipStatusResponse;
import br.com.watchusee.watchusee.friend.domain.Friendship;
import br.com.watchusee.watchusee.friend.domain.FriendshipStatus;
import br.com.watchusee.watchusee.friend.exception.FriendshipActionNotAllowedException;
import br.com.watchusee.watchusee.friend.exception.FriendshipAlreadyExistsException;
import br.com.watchusee.watchusee.friend.exception.FriendshipNotFoundException;
import br.com.watchusee.watchusee.friend.exception.SelfFriendRequestException;
import br.com.watchusee.watchusee.friend.repository.FriendshipRepository;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class FriendService {

    private static final Logger log = LoggerFactory.getLogger(FriendService.class);

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    private Clock clock = Clock.systemUTC();

    public FriendService(
            FriendshipRepository friendshipRepository,
            UserRepository userRepository
    ) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FriendRequestResponse sendRequest(Long requesterId, Long receiverId) {

        if (requesterId.equals(receiverId)) {
            throw new SelfFriendRequestException(
                    "Você não pode enviar uma solicitação de amizade para si mesmo."
            );
        }

        User requester = findUser(requesterId);
        User receiver = findUser(receiverId);

        long minId = Math.min(requesterId, receiverId);
        long maxId = Math.max(requesterId, receiverId);

        if (friendshipRepository.existsByUserMinIdAndUserMaxId(minId, maxId)) {
            throw new FriendshipAlreadyExistsException(
                    "Já existe uma relação entre esses usuários."
            );
        }

        Friendship friendship = Friendship.request(requester, receiver, Instant.now(clock));

        try {
            friendship = friendshipRepository.saveAndFlush(friendship);
        } catch (DataIntegrityViolationException exception) {

            log.debug(
                    "Corrida detectada ao criar solicitação de amizade. requesterId={} receiverId={}",
                    requesterId, receiverId
            );
            throw new FriendshipAlreadyExistsException(
                    "Já existe uma relação entre esses usuários."
            );
        }

        log.info(
                "Solicitação de amizade enviada. friendshipId={} requesterId={} receiverId={}",
                friendship.getId(), requesterId, receiverId
        );

        return new FriendRequestResponse(
                friendship.getId(),
                receiver.getId(),
                receiver.getNick(),
                friendship.getCreatedAt()
        );
    }

    @Transactional
    public void acceptRequest(Long userId, Long requestId) {

        Friendship friendship = getFriendshipOrThrow(requestId);

        if (!friendship.getReceiver().getId().equals(userId)) {
            throw new FriendshipActionNotAllowedException(
                    "Somente o destinatário pode aceitar esta solicitação."
            );
        }

        friendship.accept();

        log.info("Solicitação de amizade aceita. friendshipId={} userId={}", requestId, userId);
    }

    @Transactional
    public void rejectRequest(Long userId, Long requestId) {

        Friendship friendship = getFriendshipOrThrow(requestId);

        if (!friendship.getReceiver().getId().equals(userId)) {
            throw new FriendshipActionNotAllowedException(
                    "Somente o destinatário pode recusar esta solicitação."
            );
        }

        friendship.reject();

        log.info("Solicitação de amizade recusada. friendshipId={} userId={}", requestId, userId);
    }

    @Transactional
    public void cancelRequest(Long userId, Long requestId) {

        Friendship friendship = getFriendshipOrThrow(requestId);

        if (!friendship.getRequester().getId().equals(userId)) {
            throw new FriendshipActionNotAllowedException(
                    "Somente quem enviou a solicitação pode cancelá-la."
            );
        }

        friendship.cancel();

        log.info("Solicitação de amizade cancelada. friendshipId={} userId={}", requestId, userId);
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Operação inválida.");
        }

        long minId = Math.min(userId, friendId);
        long maxId = Math.max(userId, friendId);

        Friendship friendship = friendshipRepository
                .findByUserPair(minId, maxId)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new FriendshipNotFoundException(
                        "Amizade não encontrada."
                ));

        friendshipRepository.delete(friendship);

        log.info("Amizade removida. userId={} friendId={}", userId, friendId);
    }

    @Transactional(readOnly = true)
    public Page<FriendResponse> getFriends(Long userId, Pageable pageable) {

        return friendshipRepository
                .findAcceptedFriendships(userId, pageable)
                .map(friendship -> {
                    User friend = friendship.otherParticipant(userId);
                    return new FriendResponse(
                            friend.getId(),
                            friend.getNick(),
                            friendship.getRespondedAt()
                    );
                });
    }

    @Transactional(readOnly = true)
    public Page<FriendRequestResponse> getReceivedRequests(Long userId, Pageable pageable) {

        return friendshipRepository
                .findReceivedPendingRequests(userId, pageable)
                .map(friendship -> new FriendRequestResponse(
                        friendship.getId(),
                        friendship.getRequester().getId(),
                        friendship.getRequester().getNick(),
                        friendship.getCreatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public Page<FriendRequestResponse> getSentRequests(Long userId, Pageable pageable) {

        return friendshipRepository
                .findSentPendingRequests(userId, pageable)
                .map(friendship -> new FriendRequestResponse(
                        friendship.getId(),
                        friendship.getReceiver().getId(),
                        friendship.getReceiver().getNick(),
                        friendship.getCreatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public long countFriends(Long userId) {
        return friendshipRepository.countAcceptedFriendships(userId);
    }

    @Transactional(readOnly = true)
    public FriendshipStatusResponse getRelationshipStatus(Long currentUserId, Long targetUserId) {

        if (currentUserId.equals(targetUserId)) {
            return new FriendshipStatusResponse(targetUserId, FriendRelationStatus.SELF, null);
        }

        findUser(targetUserId);

        long minId = Math.min(currentUserId, targetUserId);
        long maxId = Math.max(currentUserId, targetUserId);

        return friendshipRepository
                .findByUserPair(minId, maxId)
                .map(friendship -> toStatusResponse(currentUserId, targetUserId, friendship))
                .orElse(new FriendshipStatusResponse(targetUserId, FriendRelationStatus.NONE, null));
    }

    private FriendshipStatusResponse toStatusResponse(
            Long currentUserId,
            Long targetUserId,
            Friendship friendship
    ) {
        FriendRelationStatus status = switch (friendship.getStatus()) {
            case ACCEPTED -> FriendRelationStatus.FRIENDS;
            case PENDING -> friendship.getRequester().getId().equals(currentUserId)
                    ? FriendRelationStatus.REQUEST_SENT
                    : FriendRelationStatus.REQUEST_RECEIVED;
            case REJECTED -> FriendRelationStatus.REJECTED;
            case CANCELLED -> FriendRelationStatus.CANCELLED;
        };

        return new FriendshipStatusResponse(targetUserId, status, friendship.getId());
    }

    private Friendship getFriendshipOrThrow(Long requestId) {
        return friendshipRepository
                .findById(requestId)
                .orElseThrow(() -> new FriendshipNotFoundException(
                        "Solicitação de amizade não encontrada."
                ));
    }

    private User findUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário não encontrado: " + userId
                ));
    }
}
