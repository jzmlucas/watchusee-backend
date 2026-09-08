package br.com.watchusee.watchusee.friend.repository;

import br.com.watchusee.watchusee.friend.domain.Friendship;
import br.com.watchusee.watchusee.friend.domain.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository
        extends JpaRepository<Friendship, Long> {

    /**
     * Verifica se existe uma amizade ACEITA entre dois usuários,
     * independentemente de quem enviou a solicitação originalmente.
     *
     * Usado para controle de acesso: só amigos podem visualizar
     * dados privados um do outro (ex.: watchlist).
     */
    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM Friendship f
            WHERE f.status = br.com.watchusee.watchusee.friend.domain.FriendshipStatus.ACCEPTED
              AND (
                    (f.requester.id = :userIdA AND f.receiver.id = :userIdB)
                 OR (f.requester.id = :userIdB AND f.receiver.id = :userIdA)
              )
            """)
    boolean existsAcceptedFriendshipBetween(
            @Param("userIdA") Long userIdA,
            @Param("userIdB") Long userIdB
    );

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