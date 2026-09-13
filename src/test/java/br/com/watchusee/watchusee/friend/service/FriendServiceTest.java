package br.com.watchusee.watchusee.friend.service;

import br.com.watchusee.watchusee.friend.api.dto.FriendRelationStatus;
import br.com.watchusee.watchusee.friend.api.dto.FriendRequestResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendshipStatusResponse;
import br.com.watchusee.watchusee.friend.domain.Friendship;
import br.com.watchusee.watchusee.friend.exception.FriendshipActionNotAllowedException;
import br.com.watchusee.watchusee.friend.exception.FriendshipAlreadyExistsException;
import br.com.watchusee.watchusee.friend.exception.FriendshipNotFoundException;
import br.com.watchusee.watchusee.friend.exception.FriendshipStateConflictException;
import br.com.watchusee.watchusee.friend.exception.SelfFriendRequestException;
import br.com.watchusee.watchusee.friend.repository.FriendshipRepository;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendService")
class FriendServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    private FriendService friendService;

    private User buildUser(Long id, String nick) {
        User user = new User(nick, "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Friendship buildFriendship(Long id, User requester, User receiver) {
        Friendship friendship = Friendship.request(requester, receiver, Instant.now());
        ReflectionTestUtils.setField(friendship, "id", id);
        return friendship;
    }

    @BeforeEach
    void setUp() {
        friendService = new FriendService(friendshipRepository, userRepository);
    }

    @Nested
    @DisplayName("sendRequest()")
    class SendRequest {

        @Test
        @DisplayName("deve criar a solicitação quando os usuários são válidos e não há relação prévia")
        void shouldSendRequest() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");

            when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(friendshipRepository.existsByUserMinIdAndUserMaxId(1L, 2L)).thenReturn(false);
            when(friendshipRepository.saveAndFlush(any(Friendship.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            FriendRequestResponse response = friendService.sendRequest(1L, 2L);

            assertThat(response.userId()).isEqualTo(2L);
            assertThat(response.nick()).isEqualTo("maria");
            verify(friendshipRepository).saveAndFlush(any(Friendship.class));
        }

        @Test
        @DisplayName("não deve permitir que o usuário envie solicitação para si mesmo")
        void shouldNotAllowSelfRequest() {

            assertThatThrownBy(() -> friendService.sendRequest(1L, 1L))
                    .isInstanceOf(SelfFriendRequestException.class);

            verify(friendshipRepository, never()).saveAndFlush(any());
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("deve lançar 404 quando o destinatário não existe")
        void shouldThrowWhenReceiverDoesNotExist() {

            when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "lucas")));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendService.sendRequest(1L, 2L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(friendshipRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("deve lançar conflito quando já existe QUALQUER relação entre os usuários, " +
                "independentemente de quem enviou originalmente")
        void shouldThrowWhenRelationAlreadyExists() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");

            when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));


            when(friendshipRepository.existsByUserMinIdAndUserMaxId(1L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> friendService.sendRequest(2L, 1L))
                    .isInstanceOf(FriendshipAlreadyExistsException.class);

            verify(friendshipRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("deve converter uma violação de constraint única (corrida) em conflito de negócio")
        void shouldTranslateRaceConditionIntoBusinessConflict() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");

            when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(friendshipRepository.existsByUserMinIdAndUserMaxId(1L, 2L)).thenReturn(false);
            when(friendshipRepository.saveAndFlush(any(Friendship.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_friendship_pair"));

            assertThatThrownBy(() -> friendService.sendRequest(1L, 2L))
                    .isInstanceOf(FriendshipAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("acceptRequest()")
    class AcceptRequest {

        @Test
        @DisplayName("deve aceitar quando o usuário autenticado é o destinatário")
        void shouldAcceptWhenUserIsReceiver() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            friendService.acceptRequest(2L, 10L);

            assertThat(friendship.getStatus().name()).isEqualTo("ACCEPTED");
            assertThat(friendship.getRespondedAt()).isNotNull();
        }

        @Test
        @DisplayName("NÃO deve permitir que quem não é o destinatário aceite (403)")
        void shouldNotAllowNonReceiverToAccept() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            assertThatThrownBy(() -> friendService.acceptRequest(99L, 10L))
                    .isInstanceOf(FriendshipActionNotAllowedException.class);

            assertThat(friendship.getStatus().name()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("NÃO deve permitir aceitar uma solicitação já recusada")
        void shouldNotAcceptRejectedRequest() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);
            friendship.reject();

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            assertThatThrownBy(() -> friendService.acceptRequest(2L, 10L))
                    .isInstanceOf(FriendshipStateConflictException.class);
        }

        @Test
        @DisplayName("NÃO deve permitir aceitar uma solicitação já cancelada")
        void shouldNotAcceptCancelledRequest() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);
            friendship.cancel();

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            assertThatThrownBy(() -> friendService.acceptRequest(2L, 10L))
                    .isInstanceOf(FriendshipStateConflictException.class);
        }

        @Test
        @DisplayName("deve lançar 404 quando a solicitação não existe")
        void shouldThrowWhenRequestNotFound() {

            when(friendshipRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendService.acceptRequest(1L, 404L))
                    .isInstanceOf(FriendshipNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("rejectRequest()")
    class RejectRequest {

        @Test
        @DisplayName("deve recusar quando o usuário autenticado é o destinatário")
        void shouldRejectWhenUserIsReceiver() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            friendService.rejectRequest(2L, 10L);

            assertThat(friendship.getStatus().name()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("NÃO deve permitir que quem enviou recuse a própria solicitação")
        void shouldNotAllowRequesterToReject() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            assertThatThrownBy(() -> friendService.rejectRequest(1L, 10L))
                    .isInstanceOf(FriendshipActionNotAllowedException.class);
        }
    }

    @Nested
    @DisplayName("cancelRequest()")
    class CancelRequest {

        @Test
        @DisplayName("deve cancelar quando o usuário autenticado é quem enviou")
        void shouldCancelWhenUserIsRequester() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            friendService.cancelRequest(1L, 10L);

            assertThat(friendship.getStatus().name()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("NÃO deve permitir que o destinatário cancele a solicitação (isso é 'recusar')")
        void shouldNotAllowReceiverToCancel() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            assertThatThrownBy(() -> friendService.cancelRequest(2L, 10L))
                    .isInstanceOf(FriendshipActionNotAllowedException.class);
        }

        @Test
        @DisplayName("NÃO deve permitir cancelar uma solicitação já aceita")
        void shouldNotCancelAcceptedRequest() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);
            friendship.accept();

            when(friendshipRepository.findById(10L)).thenReturn(Optional.of(friendship));

            assertThatThrownBy(() -> friendService.cancelRequest(1L, 10L))
                    .isInstanceOf(FriendshipStateConflictException.class);
        }
    }

    @Nested
    @DisplayName("removeFriend()")
    class RemoveFriend {

        @Test
        @DisplayName("deve remover quando existe amizade ACEITA entre os usuários")
        void shouldRemoveAcceptedFriendship() {

            User self = buildUser(1L, "lucas");
            User friend = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, self, friend);
            friendship.accept();

            when(friendshipRepository.findByUserPair(1L, 2L)).thenReturn(Optional.of(friendship));

            friendService.removeFriend(1L, 2L);

            verify(friendshipRepository).delete(friendship);
        }

        @Test
        @DisplayName("deve lançar 404 ao tentar remover uma amizade que não existe")
        void shouldThrowWhenFriendshipDoesNotExist() {

            when(friendshipRepository.findByUserPair(1L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendService.removeFriend(1L, 2L))
                    .isInstanceOf(FriendshipNotFoundException.class);

            verify(friendshipRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lançar 404 ao tentar remover uma relação que ainda está apenas pendente")
        void shouldThrowWhenFriendshipIsOnlyPending() {

            User self = buildUser(1L, "lucas");
            User friend = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, self, friend);

            when(friendshipRepository.findByUserPair(1L, 2L)).thenReturn(Optional.of(friendship));

            assertThatThrownBy(() -> friendService.removeFriend(1L, 2L))
                    .isInstanceOf(FriendshipNotFoundException.class);

            verify(friendshipRepository, never()).delete(any());
        }

        @Test
        @DisplayName("não deve aceitar remover a si mesmo")
        void shouldNotAllowRemovingSelf() {

            assertThatThrownBy(() -> friendService.removeFriend(1L, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Listagens paginadas e contagem")
    class Queries {

        @Test
        @DisplayName("getFriends() deve retornar o outro usuário da amizade, não o próprio usuário")
        void shouldReturnOtherUserInFriendship() {

            User self = buildUser(1L, "lucas");
            User friend = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, self, friend);
            friendship.accept();

            Pageable pageable = PageRequest.of(0, 20);
            Page<Friendship> page = new PageImpl<>(List.of(friendship), pageable, 1);

            when(friendshipRepository.findAcceptedFriendships(1L, pageable)).thenReturn(page);

            Page<FriendResponse> friends = friendService.getFriends(1L, pageable);

            assertThat(friends.getContent()).hasSize(1);
            assertThat(friends.getContent().get(0).id()).isEqualTo(2L);
            assertThat(friends.getContent().get(0).nick()).isEqualTo("maria");
        }

        @Test
        @DisplayName("getReceivedRequests() deve expor o requester como o outro usuário")
        void shouldExposeRequesterInReceivedRequests() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Friendship> page = new PageImpl<>(List.of(friendship), pageable, 1);

            when(friendshipRepository.findReceivedPendingRequests(2L, pageable)).thenReturn(page);

            Page<FriendRequestResponse> requests = friendService.getReceivedRequests(2L, pageable);

            assertThat(requests.getContent()).hasSize(1);
            assertThat(requests.getContent().get(0).userId()).isEqualTo(1L);
            assertThat(requests.getContent().get(0).nick()).isEqualTo("lucas");
        }

        @Test
        @DisplayName("getSentRequests() deve expor o receiver como o outro usuário")
        void shouldExposeReceiverInSentRequests() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Friendship> page = new PageImpl<>(List.of(friendship), pageable, 1);

            when(friendshipRepository.findSentPendingRequests(1L, pageable)).thenReturn(page);

            Page<FriendRequestResponse> requests = friendService.getSentRequests(1L, pageable);

            assertThat(requests.getContent()).hasSize(1);
            assertThat(requests.getContent().get(0).userId()).isEqualTo(2L);
            assertThat(requests.getContent().get(0).nick()).isEqualTo("maria");
        }

        @Test
        @DisplayName("countFriends() deve delegar diretamente à contagem de amizades ACEITAS")
        void shouldCountAcceptedFriendships() {

            when(friendshipRepository.countAcceptedFriendships(1L)).thenReturn(3L);

            assertThat(friendService.countFriends(1L)).isEqualTo(3L);
        }

        @Test
        @DisplayName("getRelationshipStatus() deve retornar SELF quando o usuário consulta a si mesmo")
        void shouldReturnSelfStatus() {

            FriendshipStatusResponse response = friendService.getRelationshipStatus(1L, 1L);

            assertThat(response.status()).isEqualTo(FriendRelationStatus.SELF);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("getRelationshipStatus() deve retornar NONE quando não existe relação entre os usuários")
        void shouldReturnNoneWhenNoRelationExists() {

            when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, "maria")));
            when(friendshipRepository.findByUserPair(1L, 2L)).thenReturn(Optional.empty());

            FriendshipStatusResponse response = friendService.getRelationshipStatus(1L, 2L);

            assertThat(response.status()).isEqualTo(FriendRelationStatus.NONE);
        }

        @Test
        @DisplayName("getRelationshipStatus() deve lançar 404 quando o usuário-alvo não existe")
        void shouldThrowWhenTargetUserDoesNotExist() {

            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendService.getRelationshipStatus(1L, 2L))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("getRelationshipStatus() deve retornar REQUEST_SENT para quem enviou e " +
                "REQUEST_RECEIVED para quem recebeu, para a mesma solicitação pendente")
        void shouldDistinguishSentFromReceived() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
            when(friendshipRepository.findByUserPair(1L, 2L)).thenReturn(Optional.of(friendship));

            FriendshipStatusResponse fromRequesterPerspective =
                    friendService.getRelationshipStatus(1L, 2L);
            FriendshipStatusResponse fromReceiverPerspective =
                    friendService.getRelationshipStatus(2L, 1L);

            assertThat(fromRequesterPerspective.status()).isEqualTo(FriendRelationStatus.REQUEST_SENT);
            assertThat(fromReceiverPerspective.status()).isEqualTo(FriendRelationStatus.REQUEST_RECEIVED);
        }

        @Test
        @DisplayName("getRelationshipStatus() deve retornar FRIENDS quando a relação está ACEITA")
        void shouldReturnFriendsStatus() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);
            friendship.accept();

            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(friendshipRepository.findByUserPair(1L, 2L)).thenReturn(Optional.of(friendship));

            FriendshipStatusResponse response = friendService.getRelationshipStatus(1L, 2L);

            assertThat(response.status()).isEqualTo(FriendRelationStatus.FRIENDS);
        }

        @Test
        @DisplayName("getRelationshipStatus() deve retornar CANCELLED quando a solicitação foi cancelada")
        void shouldReturnCancelledStatus() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);
            friendship.cancel();

            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(friendshipRepository.findByUserPair(1L, 2L)).thenReturn(Optional.of(friendship));

            FriendshipStatusResponse response = friendService.getRelationshipStatus(1L, 2L);

            assertThat(response.status()).isEqualTo(FriendRelationStatus.CANCELLED);
        }
    }
}
