package br.com.watchusee.watchusee.friend.service;

import br.com.watchusee.watchusee.friend.api.dto.FriendRelationStatus;
import br.com.watchusee.watchusee.friend.api.dto.FriendResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendshipStatusResponse;
import br.com.watchusee.watchusee.friend.domain.Friendship;
import br.com.watchusee.watchusee.friend.repository.FriendshipRepository;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

    private Friendship buildFriendship(
            Long id, User requester, User receiver
    ) {
        Friendship friendship = new Friendship(requester, receiver);
        ReflectionTestUtils.setField(friendship, "id", id);
        return friendship;
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        friendService = new FriendService(friendshipRepository, userRepository);
    }

    @Nested
    @DisplayName("sendRequest()")
    class SendRequest {

        @Test
        @DisplayName("deve criar a solicitação quando os usuários são válidos")
        void shouldSendRequest() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");

            when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));

            when(friendshipRepository
                    .findByRequesterIdAndReceiverId(1L, 2L))
                    .thenReturn(Optional.empty());
            when(friendshipRepository
                    .findByRequesterIdAndReceiverId(2L, 1L))
                    .thenReturn(Optional.empty());

            friendService.sendRequest(1L, 2L);

            verify(friendshipRepository).save(any(Friendship.class));
        }

        @Test
        @DisplayName("não deve permitir que o usuário envie solicitação para si mesmo")
        void shouldNotAllowSelfRequest() {

            assertThatThrownBy(() ->
                    friendService.sendRequest(1L, 1L)
            ).isInstanceOf(IllegalArgumentException.class);

            verify(friendshipRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando já existe solicitação entre os usuários")
        void shouldThrowWhenRequestAlreadyExists() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");

            when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));

            when(friendshipRepository
                    .findByRequesterIdAndReceiverId(1L, 2L))
                    .thenReturn(Optional.of(
                            buildFriendship(10L, requester, receiver)
                    ));

            assertThatThrownBy(() ->
                    friendService.sendRequest(1L, 2L)
            ).isInstanceOf(IllegalStateException.class);

            verify(friendshipRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("acceptRequest() / rejectRequest()")
    class AcceptReject {

        @Test
        @DisplayName("deve aceitar a solicitação quando o usuário autenticado é o destinatário")
        void shouldAcceptWhenUserIsReceiver() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L))
                    .thenReturn(Optional.of(friendship));

            friendService.acceptRequest(2L, 10L);

            assertThat(friendship.getStatus().name()).isEqualTo("ACCEPTED");
        }

        @Test
        @DisplayName("NÃO deve permitir que outro usuário (que não o destinatário) aceite a solicitação")
        void shouldNotAllowNonReceiverToAccept() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L))
                    .thenReturn(Optional.of(friendship));

            assertThatThrownBy(() ->
                    friendService.acceptRequest(99L, 10L)
            ).isInstanceOf(IllegalStateException.class);

            assertThat(friendship.getStatus().name()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("NÃO deve permitir aceitar uma solicitação que não está pendente")
        void shouldNotAcceptNonPendingRequest() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);
            friendship.accept();

            when(friendshipRepository.findById(10L))
                    .thenReturn(Optional.of(friendship));

            assertThatThrownBy(() ->
                    friendService.acceptRequest(2L, 10L)
            ).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("NÃO deve permitir que outro usuário recuse a solicitação")
        void shouldNotAllowNonReceiverToReject() {

            User requester = buildUser(1L, "lucas");
            User receiver = buildUser(2L, "maria");
            Friendship friendship = buildFriendship(10L, requester, receiver);

            when(friendshipRepository.findById(10L))
                    .thenReturn(Optional.of(friendship));

            assertThatThrownBy(() ->
                    friendService.rejectRequest(99L, 10L)
            ).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("deve lançar exceção quando a solicitação não existe")
        void shouldThrowWhenRequestNotFound() {

            when(friendshipRepository.findById(404L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    friendService.acceptRequest(1L, 404L)
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getFriends() / getRelationshipStatus()")
    class Queries {

        @Test
        @DisplayName("deve retornar o outro usuário da amizade, não o próprio usuário")
        void shouldReturnOtherUserInFriendship() {

            User self = buildUser(1L, "lucas");
            User friend = buildUser(2L, "maria");

            Friendship friendship = buildFriendship(10L, self, friend);
            friendship.accept();

            when(friendshipRepository
                    .findByRequesterIdOrReceiverIdAndStatus(
                            1L, 1L, br.com.watchusee.watchusee.friend.domain.FriendshipStatus.ACCEPTED
                    )
            ).thenReturn(List.of(friendship));

            List<FriendResponse> friends = friendService.getFriends(1L);

            assertThat(friends).hasSize(1);
            assertThat(friends.get(0).id()).isEqualTo(2L);
        }

        @Test
        @DisplayName("deve retornar SELF quando o usuário consulta o próprio status")
        void shouldReturnSelfStatus() {

            lenient().when(userRepository.findById(1L))
                    .thenReturn(Optional.of(buildUser(1L, "lucas")));

            FriendshipStatusResponse response =
                    friendService.getRelationshipStatus(1L, 1L);

            assertThat(response.status()).isEqualTo(FriendRelationStatus.SELF);
        }

        @Test
        @DisplayName("deve retornar NONE quando não existe relação entre os usuários")
        void shouldReturnNoneWhenNoRelationExists() {

            when(userRepository.findById(2L))
                    .thenReturn(Optional.of(buildUser(2L, "maria")));

            when(friendshipRepository.findByRequesterIdAndReceiverId(1L, 2L))
                    .thenReturn(Optional.empty());
            when(friendshipRepository.findByRequesterIdAndReceiverId(2L, 1L))
                    .thenReturn(Optional.empty());

            FriendshipStatusResponse response =
                    friendService.getRelationshipStatus(1L, 2L);

            assertThat(response.status()).isEqualTo(FriendRelationStatus.NONE);
        }
    }
}
