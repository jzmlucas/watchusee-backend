package br.com.watchusee.watchusee.watchlist.service;

import br.com.watchusee.watchusee.friend.repository.FriendshipRepository;
import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.movie.repository.MovieRepository;
import br.com.watchusee.watchusee.movie.service.MovieService;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import br.com.watchusee.watchusee.watchlist.domain.Watchlist;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import br.com.watchusee.watchusee.watchlist.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchlistService")
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieService movieService;

    @Mock
    private FriendshipRepository friendshipRepository;

    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(
                watchlistRepository,
                userRepository,
                movieRepository,
                movieService,
                friendshipRepository
        );
    }

    private User buildUser(Long id, String nick) {
        User user = new User(nick, "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Movie buildMovie(Long id) {
        return new Movie(
                id,
                "Filme de Teste",
                "Overview de teste.",
                null,
                null,
                7.5
        );
    }

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("deve criar um novo item quando o filme ainda não está na watchlist")
        void shouldCreateNewWatchlistItem() {

            User user = buildUser(1L, "lucas");
            Movie movie = buildMovie(550L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(movieRepository.findById(550L)).thenReturn(Optional.of(movie));
            when(watchlistRepository.findByUserIdAndMovieId(1L, 550L))
                    .thenReturn(Optional.empty());
            when(watchlistRepository.save(any(Watchlist.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Watchlist result = watchlistService.updateStatus(
                    1L, 550L, WatchlistStatus.TO_WATCH
            );

            assertThat(result.getStatus()).isEqualTo(WatchlistStatus.TO_WATCH);
            assertThat(result.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("deve atualizar o status quando o item já existe na watchlist")
        void shouldUpdateExistingWatchlistItem() {

            User user = buildUser(1L, "lucas");
            Movie movie = buildMovie(550L);
            Watchlist existing = new Watchlist(user, movie, WatchlistStatus.TO_WATCH);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(movieRepository.findById(550L)).thenReturn(Optional.of(movie));
            when(watchlistRepository.findByUserIdAndMovieId(1L, 550L))
                    .thenReturn(Optional.of(existing));
            when(watchlistRepository.save(any(Watchlist.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Watchlist result = watchlistService.updateStatus(
                    1L, 550L, WatchlistStatus.WATCHED
            );

            assertThat(result.getStatus()).isEqualTo(WatchlistStatus.WATCHED);
            verify(watchlistRepository, times(1)).save(existing);
        }

        @Test
        @DisplayName("deve buscar o filme no TMDB quando ele não existe na base local")
        void shouldFetchMovieFromTmdbWhenNotCached() {

            User user = buildUser(1L, "lucas");
            Movie movieFromTmdb = buildMovie(999L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(movieRepository.findById(999L)).thenReturn(Optional.empty());
            when(movieService.getMovie(999L)).thenReturn(movieFromTmdb);
            when(movieRepository.save(movieFromTmdb)).thenReturn(movieFromTmdb);
            when(watchlistRepository.findByUserIdAndMovieId(1L, 999L))
                    .thenReturn(Optional.empty());
            when(watchlistRepository.save(any(Watchlist.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            watchlistService.updateStatus(1L, 999L, WatchlistStatus.TO_WATCH);

            verify(movieService).getMovie(999L);
            verify(movieRepository).save(movieFromTmdb);
        }
    }

    @Nested
    @DisplayName("get() / remove()")
    class GetAndRemove {

        @Test
        @DisplayName("deve lançar exceção quando o filme não está na watchlist do usuário")
        void shouldThrowWhenItemNotFound() {

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(buildUser(1L, "lucas")));
            when(watchlistRepository.findByUserIdAndMovieId(1L, 550L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    watchlistService.get(1L, 550L)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("remove() deve delegar ao repositório escopado pelo próprio userId")
        void shouldRemoveScopedToOwnUser() {

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(buildUser(1L, "lucas")));

            watchlistService.remove(1L, 550L);

            verify(watchlistRepository).deleteByUserIdAndMovieId(1L, 550L);
        }
    }

    @Nested
    @DisplayName("findAllForViewer() — controle de acesso (IDOR)")
    class FindAllForViewer {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        @DisplayName("[IDOR] deve permitir que o usuário visualize a própria watchlist")
        void shouldAllowViewingOwnWatchlist() {

            User self = buildUser(1L, "lucas");

            when(userRepository.findById(1L)).thenReturn(Optional.of(self));

            Page<Watchlist> emptyPage = new PageImpl<>(List.of());
            when(watchlistRepository.findAllByUserId(1L, pageable))
                    .thenReturn(emptyPage);

            watchlistService.findAllForViewer(1L, 1L, null, pageable);

            verify(friendshipRepository, never())
                    .existsAcceptedFriendshipBetween(any(), any());
        }

        @Test
        @DisplayName("[IDOR] deve permitir a visualização quando os usuários são amigos")
        void shouldAllowViewingWhenUsersAreFriends() {

            User target = buildUser(2L, "maria");

            when(userRepository.findById(2L)).thenReturn(Optional.of(target));
            when(friendshipRepository.existsAcceptedFriendshipBetween(1L, 2L))
                    .thenReturn(true);

            when(watchlistRepository.findAllByUserId(2L, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            watchlistService.findAllForViewer(1L, 2L, null, pageable);

            verify(friendshipRepository).existsAcceptedFriendshipBetween(1L, 2L);
        }

        @Test
        @DisplayName("[IDOR] NÃO deve permitir visualizar a watchlist de um usuário que não é amigo")
        void shouldDenyViewingWhenUsersAreNotFriends() {

            User target = buildUser(2L, "maria");

            when(userRepository.findById(2L)).thenReturn(Optional.of(target));
            when(friendshipRepository.existsAcceptedFriendshipBetween(1L, 2L))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                    watchlistService.findAllForViewer(1L, 2L, null, pageable)
            ).isInstanceOf(AccessDeniedException.class);

            verify(watchlistRepository, never())
                    .findAllByUserId(any(), any());
            verify(watchlistRepository, never())
                    .findAllByUserIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("[IDOR] deve negar acesso mesmo quando existe amizade apenas PENDENTE (não aceita)")
        void shouldDenyAccessWhenFriendshipIsOnlyPending() {

            User target = buildUser(2L, "maria");

            when(userRepository.findById(2L)).thenReturn(Optional.of(target));

            when(friendshipRepository.existsAcceptedFriendshipBetween(1L, 2L))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                    watchlistService.findAllForViewer(1L, 2L, null, pageable)
            ).isInstanceOf(AccessDeniedException.class);
        }
    }
}
