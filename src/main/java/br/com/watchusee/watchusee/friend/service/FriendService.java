package br.com.watchusee.watchusee.friend.service;

import br.com.watchusee.watchusee.friend.api.dto.FriendRelationStatus;
import br.com.watchusee.watchusee.friend.api.dto.FriendRequestResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendshipStatusResponse;
import br.com.watchusee.watchusee.friend.domain.Friendship;
import br.com.watchusee.watchusee.friend.domain.FriendshipStatus;
import br.com.watchusee.watchusee.friend.repository.FriendshipRepository;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendService(
            FriendshipRepository friendshipRepository,
            UserRepository userRepository
    ) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void sendRequest(
            Long requesterId,
            Long receiverId
    ) {

        validateUsers(requesterId, receiverId);

        if (requesterId.equals(receiverId)) {
            throw new IllegalArgumentException(
                    "Você não pode adicionar a si mesmo."
            );
        }

        User requester =
                findUser(requesterId);

        User receiver =
                findUser(receiverId);

        if (friendshipRepository
                .findByRequesterIdAndReceiverId(
                        requesterId,
                        receiverId
                )
                .isPresent()) {

            throw new IllegalStateException(
                    "Já existe uma solicitação entre esses usuários."
            );
        }

        if (friendshipRepository
                .findByRequesterIdAndReceiverId(
                        receiverId,
                        requesterId
                )
                .isPresent()) {

            throw new IllegalStateException(
                    "Já existe uma solicitação entre esses usuários."
            );
        }

        Friendship friendship =
                new Friendship(
                        requester,
                        receiver
                );

        friendshipRepository.save(friendship);
    }

    @Transactional
    public void acceptRequest(
            Long userId,
            Long requestId
    ) {

        Friendship friendship =
                friendshipRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Solicitação de amizade não encontrada."
                                )
                        );

        if (!friendship
                .getReceiver()
                .getId()
                .equals(userId)) {

            throw new IllegalStateException(
                    "Você não pode aceitar esta solicitação."
            );
        }

        if (friendship.getStatus()
                != FriendshipStatus.PENDING) {

            throw new IllegalStateException(
                    "Esta solicitação não está pendente."
            );
        }

        friendship.accept();
    }

    @Transactional
    public void rejectRequest(
            Long userId,
            Long requestId
    ) {

        Friendship friendship =
                friendshipRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Solicitação de amizade não encontrada."
                                )
                        );

        if (!friendship
                .getReceiver()
                .getId()
                .equals(userId)) {

            throw new IllegalStateException(
                    "Você não pode recusar esta solicitação."
            );
        }

        if (friendship.getStatus()
                != FriendshipStatus.PENDING) {

            throw new IllegalStateException(
                    "Esta solicitação não está pendente."
            );
        }

        friendship.reject();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getReceivedRequests(
            Long userId
    ) {

        return friendshipRepository
                .findByReceiverIdAndStatus(
                        userId,
                        FriendshipStatus.PENDING
                )
                .stream()
                .map(friendship ->
                        new FriendRequestResponse(
                                friendship.getId(),
                                friendship.getRequester().getId(),
                                friendship.getRequester().getNick()
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(
            Long userId
    ) {

        List<Friendship> friendships =
                friendshipRepository
                        .findByRequesterIdOrReceiverIdAndStatus(
                                userId,
                                userId,
                                FriendshipStatus.ACCEPTED
                        );

        return friendships
                .stream()
                .map(friendship -> {

                    User friend =
                            friendship.getRequester()
                                    .getId()
                                    .equals(userId)
                                    ? friendship.getReceiver()
                                    : friendship.getRequester();

                    return new FriendResponse(
                            friend.getId(),
                            friend.getNick()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public long countFriends(
            Long userId
    ) {

        long sent =
                friendshipRepository
                        .countByRequesterIdAndStatus(
                                userId,
                                FriendshipStatus.ACCEPTED
                        );

        long received =
                friendshipRepository
                        .countByReceiverIdAndStatus(
                                userId,
                                FriendshipStatus.ACCEPTED
                        );

        return sent + received;
    }

    @Transactional(readOnly = true)
    public FriendshipStatusResponse getRelationshipStatus(
            Long currentUserId,
            Long targetUserId
    ) {

        validateUsers(currentUserId, targetUserId);

        if (currentUserId.equals(targetUserId)) {

            return new FriendshipStatusResponse(
                    targetUserId,
                    FriendRelationStatus.SELF,
                    null
            );
        }

        findUser(targetUserId);

        Optional<Friendship> asRequester =
                friendshipRepository.findByRequesterIdAndReceiverId(
                        currentUserId,
                        targetUserId
                );

        if (asRequester.isPresent()) {

            return toStatusResponse(
                    targetUserId,
                    asRequester.get(),
                    FriendRelationStatus.REQUEST_SENT
            );
        }

        Optional<Friendship> asReceiver =
                friendshipRepository.findByRequesterIdAndReceiverId(
                        targetUserId,
                        currentUserId
                );

        if (asReceiver.isPresent()) {

            return toStatusResponse(
                    targetUserId,
                    asReceiver.get(),
                    FriendRelationStatus.REQUEST_RECEIVED
            );
        }

        return new FriendshipStatusResponse(
                targetUserId,
                FriendRelationStatus.NONE,
                null
        );
    }

    private FriendshipStatusResponse toStatusResponse(
            Long targetUserId,
            Friendship friendship,
            FriendRelationStatus statusWhenPending
    ) {

        FriendRelationStatus status =
                switch (friendship.getStatus()) {
                    case ACCEPTED -> FriendRelationStatus.FRIENDS;
                    case PENDING -> statusWhenPending;
                    case REJECTED -> FriendRelationStatus.REJECTED;
                };

        return new FriendshipStatusResponse(
                targetUserId,
                status,
                friendship.getId()
        );
    }

    private User findUser(Long userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Usuário não encontrado: " + userId
                        )
                );
    }

    private void validateUsers(
            Long requesterId,
            Long receiverId
    ) {

        if (requesterId == null ||
                requesterId <= 0 ||
                receiverId == null ||
                receiverId <= 0) {

            throw new IllegalArgumentException(
                    "Usuário inválido."
            );
        }
    }
}