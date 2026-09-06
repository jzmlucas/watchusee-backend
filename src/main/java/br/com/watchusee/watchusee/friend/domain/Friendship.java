package br.com.watchusee.watchusee.friend.domain;

import br.com.watchusee.watchusee.user.domain.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "friendships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_friendship_pair",
                        columnNames = {
                                "requester_id",
                                "receiver_id"
                        }
                )
        }
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "requester_id",
            nullable = false
    )
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "receiver_id",
            nullable = false
    )
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private FriendshipStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected Friendship() {
    }

    public Friendship(
            User requester,
            User receiver
    ) {
        this.requester = requester;
        this.receiver = receiver;
        this.status = FriendshipStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public FriendshipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public void reject() {
        this.status = FriendshipStatus.REJECTED;
        this.updatedAt = Instant.now();
    }
}