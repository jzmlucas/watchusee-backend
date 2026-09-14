package br.com.watchusee.watchusee.friend.repository;

import br.com.watchusee.watchusee.friend.domain.Friendship;
import br.com.watchusee.watchusee.friend.domain.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    boolean existsByUserMinIdAndUserMaxId(Long userMinId, Long userMaxId);

    boolean existsByUserMinIdAndUserMaxIdAndStatus(
            Long userMinId,
            Long userMaxId,
            FriendshipStatus status
    );

    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.requester
            JOIN FETCH f.receiver
            WHERE f.userMinId = :userMinId AND f.userMaxId = :userMaxId
            """)
    Optional<Friendship> findByUserPair(
            @Param("userMinId") Long userMinId,
            @Param("userMaxId") Long userMaxId
    );

    @Query(
            value = """
                    SELECT f FROM Friendship f
                    JOIN FETCH f.requester
                    JOIN FETCH f.receiver
                    WHERE f.status = br.com.watchusee.watchusee.friend.domain.FriendshipStatus.ACCEPTED
                      AND (f.userMinId = :userId OR f.userMaxId = :userId)
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM Friendship f
                    WHERE f.status = br.com.watchusee.watchusee.friend.domain.FriendshipStatus.ACCEPTED
                      AND (f.userMinId = :userId OR f.userMaxId = :userId)
                    """
    )
    Page<Friendship> findAcceptedFriendships(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT COUNT(f)
        FROM Friendship f
        WHERE f.status = br.com.watchusee.watchusee.friend.domain.FriendshipStatus.ACCEPTED
          AND (f.userMinId = :userId OR f.userMaxId = :userId)
        """)
    long countAcceptedFriendships(@Param("userId") Long userId);

    @Query(
            value = """
                    SELECT f FROM Friendship f
                    JOIN FETCH f.requester
                    WHERE f.receiver.id = :userId
                      AND f.status = br.com.watchusee.watchusee.friend.domain.FriendshipStatus.PENDING
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM Friendship f
                    WHERE f.receiver.id = :userId
                      AND f.status = br.com.watchusee.watchusee.friend.domain.FriendshipStatus.PENDING
                    """
    )
    Page<Friendship> findReceivedPendingRequests(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value = """
                    SELECT f FROM Friendship f
                    JOIN FETCH f.receiver
                    WHERE f.requester.id = :userId
                      AND f.status = br.com.watchusee.watchusee.friend.domain.FriendshipStatus.PENDING
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM Friendship f
                    WHERE f.requester.id = :userId
                      AND f.status = br.com.watchusee.watchusee.friend.domain.FriendshipStatus.PENDING
                    """
    )
    Page<Friendship> findSentPendingRequests(@Param("userId") Long userId, Pageable pageable);

    default boolean existsAcceptedFriendshipBetween(Long userIdA, Long userIdB) {
        long min = Math.min(userIdA, userIdB);
        long max = Math.max(userIdA, userIdB);
        return existsByUserMinIdAndUserMaxIdAndStatus(min, max, FriendshipStatus.ACCEPTED);
    }
}
