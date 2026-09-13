package br.com.watchusee.watchusee.friend.domain;

import br.com.watchusee.watchusee.friend.exception.FriendshipStateConflictException;
import br.com.watchusee.watchusee.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "friendships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_friendship_pair",
                        columnNames = {"user_min_id", "user_max_id"}
                )
        },
        indexes = {
                @Index(name = "ix_friendship_receiver_status", columnList = "receiver_id,status"),
                @Index(name = "ix_friendship_requester_status", columnList = "requester_id,status")
        }
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false, updatable = false)
    private User receiver;

    @Column(name = "user_min_id", nullable = false, updatable = false)
    private Long userMinId;

    @Column(name = "user_max_id", nullable = false, updatable = false)
    private Long userMaxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendshipStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Friendship() {
    }

    private Friendship(User requester, User receiver, Instant now) {
        this.requester = Objects.requireNonNull(requester, "requester não pode ser nulo.");
        this.receiver = Objects.requireNonNull(receiver, "receiver não pode ser nulo.");

        if (requester.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException(
                    "Não é possível criar uma amizade de um usuário consigo mesmo."
            );
        }

        this.status = FriendshipStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
        normalizePair();
    }

    public static Friendship request(User requester, User receiver, Instant now) {
        return new Friendship(requester, receiver, now);
    }

    @PrePersist
    private void normalizePair() {

        long a = requester.getId();
        long b = receiver.getId();
        this.userMinId = Math.min(a, b);
        this.userMaxId = Math.max(a, b);
    }

    public void accept() {
        requireTransitionableFrom(FriendshipStatus.PENDING);
        Instant now = Instant.now();
        this.status = FriendshipStatus.ACCEPTED;
        this.respondedAt = now;
        this.updatedAt = now;
    }

    public void reject() {
        requireTransitionableFrom(FriendshipStatus.PENDING);
        Instant now = Instant.now();
        this.status = FriendshipStatus.REJECTED;
        this.respondedAt = now;
        this.updatedAt = now;
    }

    public void cancel() {
        requireTransitionableFrom(FriendshipStatus.PENDING);
        Instant now = Instant.now();
        this.status = FriendshipStatus.CANCELLED;
        this.respondedAt = now;
        this.updatedAt = now;
    }

    private void requireTransitionableFrom(FriendshipStatus expected) {
        if (this.status != expected) {
            throw new FriendshipStateConflictException(
                    "Esta solicitação não está mais pendente (status atual: " + this.status + ")."
            );
        }
    }

    public User otherParticipant(Long userId) {
        if (requester.getId().equals(userId)) {
            return receiver;
        }
        if (receiver.getId().equals(userId)) {
            return requester;
        }
        throw new IllegalArgumentException(
                "O usuário " + userId + " não participa desta relação."
        );
    }

    public boolean involves(Long userId) {
        return requester.getId().equals(userId) || receiver.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public User getRequester() {
        return requester;
    }

    public User getReceiver() {
        return receiver;
    }

    public Long getUserMinId() {
        return userMinId;
    }

    public Long getUserMaxId() {
        return userMaxId;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Long getVersion() {
        return version;
    }
}
