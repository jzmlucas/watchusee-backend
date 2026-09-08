package br.com.watchusee.watchusee.share.service;

import br.com.watchusee.watchusee.share.api.dto.CreateShareRequest;
import br.com.watchusee.watchusee.share.api.dto.ShareResponse;
import br.com.watchusee.watchusee.share.domain.Share;
import br.com.watchusee.watchusee.share.exception.ShareRecipientNotFoundException;
import br.com.watchusee.watchusee.share.repository.ShareRepository;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShareService")
class ShareServiceTest {

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private UserRepository userRepository;

    private ShareService shareService;

    @BeforeEach
    void setUp() {
        shareService = new ShareService(shareRepository, userRepository);
    }

    private User buildUser(Long id, String nick) {
        User user = new User(nick, "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Share buildShare(Long id, User sender, User recipient, Long movieId) {
        Share share = new Share(sender, recipient, movieId, "olha esse filme!");
        ReflectionTestUtils.setField(share, "id", id);
        return share;
    }

    @Nested
    @DisplayName("createShare()")
    class CreateShare {

        @Test
        @DisplayName("deve criar o compartilhamento quando remetente e destinatário são válidos")
        void shouldCreateShare() {

            User sender = buildUser(1L, "lucas");
            User recipient = buildUser(2L, "maria");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findByNick("maria")).thenReturn(Optional.of(recipient));
            when(shareRepository.existsBySenderIdAndRecipientIdAndMovieIdAndStatus(
                    any(), any(), any(), any()
            )).thenReturn(false);
            when(shareRepository.save(any(Share.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ShareResponse response = shareService.createShare(
                    1L,
                    new CreateShareRequest(550L, "maria", "olha esse filme!")
            );

            assertThat(response.senderId()).isEqualTo(1L);
            assertThat(response.recipientId()).isEqualTo(2L);
            assertThat(response.movieId()).isEqualTo(550L);
        }

        @Test
        @DisplayName("não deve permitir compartilhar um filme consigo mesmo")
        void shouldNotAllowSharingWithSelf() {

            User sender = buildUser(1L, "lucas");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findByNick("lucas")).thenReturn(Optional.of(sender));

            assertThatThrownBy(() ->
                    shareService.createShare(
                            1L,
                            new CreateShareRequest(550L, "lucas", null)
                    )
            ).isInstanceOf(IllegalArgumentException.class);

            verify(shareRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar ShareRecipientNotFoundException quando o destinatário não existe")
        void shouldThrowWhenRecipientNotFound() {

            User sender = buildUser(1L, "lucas");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findByNick("inexistente")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    shareService.createShare(
                            1L,
                            new CreateShareRequest(550L, "inexistente", null)
                    )
            ).isInstanceOf(ShareRecipientNotFoundException.class);
        }

        @Test
        @DisplayName("não deve permitir compartilhar o mesmo filme duas vezes enquanto pendente")
        void shouldNotAllowDuplicatePendingShare() {

            User sender = buildUser(1L, "lucas");
            User recipient = buildUser(2L, "maria");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findByNick("maria")).thenReturn(Optional.of(recipient));
            when(shareRepository.existsBySenderIdAndRecipientIdAndMovieIdAndStatus(
                    1L, 2L, 550L,
                    br.com.watchusee.watchusee.share.domain.ShareStatus.PENDING
            )).thenReturn(true);

            assertThatThrownBy(() ->
                    shareService.createShare(
                            1L,
                            new CreateShareRequest(550L, "maria", null)
                    )
            ).isInstanceOf(IllegalArgumentException.class);

            verify(shareRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("acceptShare() / rejectShare()")
    class AcceptReject {

        @Test
        @DisplayName("deve aceitar o compartilhamento quando o usuário é o destinatário")
        void shouldAcceptShare() {

            User sender = buildUser(1L, "lucas");
            User recipient = buildUser(2L, "maria");
            Share share = buildShare(100L, sender, recipient, 550L);

            when(shareRepository.findByIdAndRecipientId(100L, 2L))
                    .thenReturn(Optional.of(share));

            ShareResponse response = shareService.acceptShare(2L, 100L);

            assertThat(response.status())
                    .isEqualTo(br.com.watchusee.watchusee.share.domain.ShareStatus.ACCEPTED);
        }

        @Test
        @DisplayName("NÃO deve permitir que um usuário que não é o destinatário aceite o compartilhamento")
        void shouldNotAllowNonRecipientToAccept() {

            when(shareRepository.findByIdAndRecipientId(100L, 99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    shareService.acceptShare(99L, 100L)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("não deve aceitar um compartilhamento que já foi processado")
        void shouldNotAcceptAlreadyProcessedShare() {

            User sender = buildUser(1L, "lucas");
            User recipient = buildUser(2L, "maria");
            Share share = buildShare(100L, sender, recipient, 550L);
            share.accept();

            when(shareRepository.findByIdAndRecipientId(100L, 2L))
                    .thenReturn(Optional.of(share));

            assertThatThrownBy(() ->
                    shareService.acceptShare(2L, 100L)
            ).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("deve recusar o compartilhamento quando o usuário é o destinatário")
        void shouldRejectShare() {

            User sender = buildUser(1L, "lucas");
            User recipient = buildUser(2L, "maria");
            Share share = buildShare(100L, sender, recipient, 550L);

            when(shareRepository.findByIdAndRecipientId(100L, 2L))
                    .thenReturn(Optional.of(share));

            ShareResponse response = shareService.rejectShare(2L, 100L);

            assertThat(response.status())
                    .isEqualTo(br.com.watchusee.watchusee.share.domain.ShareStatus.REJECTED);
        }
    }
}
