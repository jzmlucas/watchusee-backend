package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.movie.repository.MovieRepository;
import br.com.watchusee.watchusee.movie.service.MovieService;
import br.com.watchusee.watchusee.user.api.dto.ChangePasswordRequest;
import br.com.watchusee.watchusee.user.api.dto.CreateUserRequest;
import br.com.watchusee.watchusee.user.api.dto.UserResponse;
import br.com.watchusee.watchusee.user.api.dto.UserSearchResponse;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.exception.InvalidCredentialsException;
import br.com.watchusee.watchusee.user.exception.UserAlreadyExistsException;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieService movieService;

    private UserService userService;

    private UserService buildService() {
        return new UserService(
                userRepository,
                passwordEncoder,
                movieRepository,
                movieService
        );
    }

    private User buildUser(Long id, String nick, String passwordHash) {
        User user = new User(nick, passwordHash);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("deve criar o usuário com a senha criptografada")
        void shouldCreateUser() {

            userService = buildService();

            CreateUserRequest request =
                    new CreateUserRequest("lucas", "senha123");

            when(userRepository.existsByNick("lucas")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hashed");

            User savedUser = buildUser(1L, "lucas", "hashed");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserResponse response = userService.createUser(request);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.nick()).isEqualTo("lucas");
        }

        @Test
        @DisplayName("deve lançar UserAlreadyExistsException quando o nick já existe")
        void shouldThrowWhenNickAlreadyExists() {

            userService = buildService();

            when(userRepository.existsByNick("lucas")).thenReturn(true);

            assertThatThrownBy(() ->
                    userService.createUser(
                            new CreateUserRequest("lucas", "senha123")
                    )
            ).isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve normalizar (trim) o nick antes de verificar duplicidade")
        void shouldTrimNickBeforeChecking() {

            userService = buildService();

            when(userRepository.existsByNick("lucas")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any(User.class)))
                    .thenReturn(buildUser(1L, "lucas", "hashed"));

            userService.createUser(new CreateUserRequest("  lucas  ", "senha123"));

            verify(userRepository).existsByNick("lucas");
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("deve alterar a senha e invalidar tokens antigos")
        void shouldChangePassword() {

            userService = buildService();

            User user = buildUser(1L, "lucas", "hashOld");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("senhaAtual", "hashOld")).thenReturn(true);
            when(passwordEncoder.matches("novaSenha1", "hashOld")).thenReturn(false);
            when(passwordEncoder.encode("novaSenha1")).thenReturn("hashNew");

            userService.changePassword(
                    1L,
                    new ChangePasswordRequest("senhaAtual", "novaSenha1")
            );

            assertThat(user.getPasswordHash()).isEqualTo("hashNew");
            assertThat(user.getTokensValidAfter()).isNotNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando a senha atual está incorreta")
        void shouldThrowWhenCurrentPasswordIsWrong() {

            userService = buildService();

            User user = buildUser(1L, "lucas", "hashOld");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("senhaErrada", "hashOld")).thenReturn(false);

            assertThatThrownBy(() ->
                    userService.changePassword(
                            1L,
                            new ChangePasswordRequest("senhaErrada", "novaSenha1")
                    )
            ).isInstanceOf(InvalidCredentialsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve rejeitar quando a nova senha é igual à atual")
        void shouldRejectSamePassword() {

            userService = buildService();

            User user = buildUser(1L, "lucas", "hashOld");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("senhaAtual", "hashOld")).thenReturn(true);

            assertThatThrownBy(() ->
                    userService.changePassword(
                            1L,
                            new ChangePasswordRequest("senhaAtual", "senhaAtual")
                    )
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve lançar UserNotFoundException quando o usuário não existe")
        void shouldThrowWhenUserNotFound() {

            userService = buildService();

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.changePassword(
                            99L,
                            new ChangePasswordRequest("qualquer1", "novaSenha1")
                    )
            ).isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("searchUsers()")
    class SearchUsers {

        @Test
        @DisplayName("não deve incluir o próprio usuário autenticado nos resultados")
        void shouldExcludeAuthenticatedUserFromResults() {

            userService = buildService();

            User self = buildUser(1L, "lucas", "hash");
            User other = buildUser(2L, "lucas2", "hash");

            when(userRepository
                    .findTop20ByNickContainingIgnoreCaseOrderByNickAsc("lucas")
            ).thenReturn(List.of(self, other));

            List<UserSearchResponse> results =
                    userService.searchUsers(1L, "lucas");

            assertThat(results)
                    .extracting(UserSearchResponse::id)
                    .containsExactly(2L);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando a query é em branco")
        void shouldReturnEmptyListForBlankQuery() {

            userService = buildService();

            List<UserSearchResponse> results =
                    userService.searchUsers(1L, "   ");

            assertThat(results).isEmpty();
            verify(userRepository, never())
                    .findTop20ByNickContainingIgnoreCaseOrderByNickAsc(any());
        }
    }
}
