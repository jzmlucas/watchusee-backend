package br.com.watchusee.watchusee.share.repository;

import br.com.watchusee.watchusee.share.domain.Share;
import br.com.watchusee.watchusee.share.domain.ShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareRepository
        extends JpaRepository<Share, Long> {

    List<Share> findByRecipientIdOrderByCreatedAtDesc(
            Long recipientId
    );

    List<Share> findBySenderIdOrderByCreatedAtDesc(
            Long senderId
    );

    List<Share> findByRecipientIdAndStatusOrderByCreatedAtDesc(
            Long recipientId,
            ShareStatus status
    );

    Optional<Share> findByIdAndRecipientId(
            Long shareId,
            Long recipientId
    );

    Optional<Share> findByIdAndSenderId(
            Long shareId,
            Long senderId
    );

    boolean existsBySenderIdAndRecipientIdAndMovieIdAndStatus(
            Long senderId,
            Long recipientId,
            Long movieId,
            ShareStatus status
    );
}