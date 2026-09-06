package br.com.watchusee.watchusee.friend.repository;

import br.com.watchusee.watchusee.friend.domain.Friendship;
import br.com.watchusee.watchusee.friend.domain.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository
        extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequesterIdAndReceiverId(
            Long requesterId,
            Long receiverId
    );

    boolean existsByRequesterIdAndReceiverId(
            Long requesterId,
            Long receiverId
    );

    long countByRequesterIdAndStatus(
            Long requesterId,
            FriendshipStatus status
    );

    long countByReceiverIdAndStatus(
            Long receiverId,
            FriendshipStatus status
    );

    List<Friendship> findByReceiverIdAndStatus(
            Long receiverId,
            FriendshipStatus status
    );

    List<Friendship> findByRequesterIdAndStatus(
            Long requesterId,
            FriendshipStatus status
    );

    List<Friendship> findByRequesterIdOrReceiverIdAndStatus(
            Long requesterId,
            Long receiverId,
            FriendshipStatus status
    );
}