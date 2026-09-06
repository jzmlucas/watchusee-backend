package br.com.watchusee.watchusee.share.domain;

import br.com.watchusee.watchusee.user.domain.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "shares",
        indexes = {
                @Index(
                        name = "idx_share_recipient",
                        columnList = "recipient_id"
                ),
                @Index(
                        name = "idx_share_sender",
                        columnList = "sender_id"
                ),
                @Index(
                        name = "idx_share_status",
                        columnList = "status"
                )
        }
)
public class Share {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sender_id",
            nullable = false
    )
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "recipient_id",
            nullable = false
    )
    private User recipient;

    @Column(
            name = "movie_id",
            nullable = false
    )
    private Long movieId;

    @Column(
            name = "message",
            length = 500
    )
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private ShareStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected Share() {
    }

    public Share(
            User sender,
            User recipient,
            Long movieId,
            String message
    ) {

        this.sender = sender;
        this.recipient = recipient;
        this.movieId = movieId;
        this.message = message;
        this.status = ShareStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public Long getMovieId() {
        return movieId;
    }

    public String getMessage() {
        return message;
    }

    public ShareStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void accept() {
        this.status = ShareStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ShareStatus.REJECTED;
    }
}