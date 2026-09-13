package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.friend.domain.FriendshipStatus;
import br.com.watchusee.watchusee.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.api.dto.UserProfileResponse;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import br.com.watchusee.watchusee.watchlist.repository.WatchlistRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(
                userRepository,
                watchlistRepository,
                friendshipRepository
        );
    }

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("deve lancar UserNotFoundException quando usuario nao existe")
        void shouldThrowWhenUserNotFound() {
            Long nonExistentId = 999L;
            when(userRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());
            assertThatThrownBy(() -> userProfileService.getProfile(nonExistentId))
                    .isInstanceOf(UserNotFoundException.class);
        }

    }

    @Nested
    @DisplayName("Count Calculations")
    class CountCalculations {

        private Long userId = 1L;

        private User createMockUser() {
            return new User(userId, "user@email.com", "nickname",
                    List.of(), List.of(), Instant.now());
        }

        @Test
        @DisplayName("deve calcular watchedMovies corretamente")
        void shouldCalculateWatchedMoviesCorrectly() {
            // Mock para encontrar usuário
            when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(createMockUser()));
            
            // Mock específico para watchlist WATCHED
            long expectedWatchedCount = 42L;
            when(watchlistRepository.countByUserIdAndStatus(userId, WatchlistStatus.WATCHED))
                    .thenReturn(expectedWatchedCount);
            
            UserProfileResponse profile = userProfileService.getProfile(userId);
            assertThat(profile.watchedMovies()).isEqualTo(expectedWatchedCount);
        }

        @Test
        @DisplayName("deve calcular toWatchMovies corretamente")
        void shouldCalculateToWatchMoviesCorrectly() {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(createMockUser()));

            when(watchlistRepository.countByUserIdAndStatus(userId, WatchlistStatus.WATCHED))
                    .thenReturn(0L);

            when(watchlistRepository.countByUserIdAndStatus(userId, WatchlistStatus.TO_WATCH))
                    .thenReturn(15L);

            when(friendshipRepository.countByRequesterIdAndStatus(userId, FriendshipStatus.ACCEPTED))
                    .thenReturn(0L);
            when(friendshipRepository.countByReceiverIdAndStatus(userId, FriendshipStatus.ACCEPTED))
                    .thenReturn(0L);

            UserProfileResponse profile = userProfileService.getProfile(userId);
            assertThat(profile.toWatchMovies()).isEqualTo(15L);
        }

        @Test
        @DisplayName("deve calcular friendsCount corretamente")
        void shouldCalculateFriendsCountCorrectly() {
            when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(createMockUser()));
            
            long requesterCount = 3L;
            long receiverCount = 7L;
            long expectedFriendsCount = requesterCount + receiverCount;
            
            when(friendshipRepository.countByRequesterIdAndStatus(userId, FriendshipStatus.ACCEPTED))
                    .thenReturn(requesterCount);
            when(friendshipRepository.countByReceiverIdAndStatus(userId, FriendshipStatus.ACCEPTED))
                    .thenReturn(receiverCount);
            
            UserProfileResponse profile = userProfileService.getProfile(userId);
            assertThat(profile.friendsCount()).isEqualTo(expectedFriendsCount);
        }

    }
}
