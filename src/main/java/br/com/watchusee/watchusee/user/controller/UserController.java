package br.com.watchusee.watchusee.user.controller;

import br.com.watchusee.watchusee.shared.api.dto.ErrorResponse;
import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import br.com.watchusee.watchusee.user.api.dto.AvatarIconResponse;
import br.com.watchusee.watchusee.user.api.dto.ChangePasswordRequest;
import br.com.watchusee.watchusee.user.api.dto.CreateUserRequest;
import br.com.watchusee.watchusee.user.api.dto.UpdateAvatarIconRequest;
import br.com.watchusee.watchusee.user.api.dto.UpdateFavoriteMovieRequest;
import br.com.watchusee.watchusee.user.api.dto.UserResponse;
import br.com.watchusee.watchusee.user.api.dto.UserSearchResponse;
import br.com.watchusee.watchusee.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/users")
@Tag(
        name = "Users",
        description = "Operações de usuários."
)
public class UserController {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;

    public UserController(
            UserService userService,
            AuthenticatedUser authenticatedUser
    ) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar usuário",
            description = """
                    Cria um novo usuário na aplicação.

                    O nick deve possuir entre 3 e 30 caracteres.
                    A senha deve possuir entre 6 e 100 caracteres.

                    A senha é armazenada de forma segura através
                    de hash utilizando o PasswordEncoder.
                    """,
            operationId = "createUser"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário criado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = UserResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de cadastro inválidos.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Nick já cadastrado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<UserResponse> createUser(
            @RequestBody
            @Valid
            CreateUserRequest request
    ) {

        UserResponse response =
                userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar usuários",
            description = """
                    Busca usuários pelo nick.

                    A busca exige autenticação e retorna no máximo
                    20 usuários.

                    O usuário autenticado não é incluído nos resultados.
                    """,
            operationId = "searchUsers"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuários encontrados."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Query inválida."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado."
            )
    })
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam
            @Size(
                    max = 30,
                    message = "O termo de busca deve possuir no máximo 30 caracteres."
            )
            String query
    ) {

        Long userId =
                authenticatedUser.getId();

        List<UserSearchResponse> response =
                userService.searchUsers(
                        userId,
                        query
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    @Operation(
            summary = "Alterar senha",
            description = """
                    Permite que o usuário autenticado altere sua senha.

                    É necessário informar a senha atual e a nova senha.
                    A senha atual é validada antes da alteração.
                    """,
            operationId = "changePassword"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Senha alterada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Senha atual incorreta ou usuário não autenticado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<Void> changePassword(
            @RequestBody
            @Valid
            ChangePasswordRequest request
    ) {

        Long userId =
                authenticatedUser.getId();

        userService.changePassword(
                userId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avatar-icons")
    @Operation(
            summary = "Listar ícones de avatar disponíveis",
            description = """
                    Retorna o conjunto fixo de ícones que podem ser
                    usados como foto de perfil. Não há upload de
                    arquivo: o usuário escolhe um dos valores retornados
                    aqui e envia em PUT /me/avatar.
                    """,
            operationId = "listAvatarIcons"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de ícones retornada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado."
            )
    })
    public ResponseEntity<List<AvatarIconResponse>> listAvatarIcons() {

        List<AvatarIconResponse> icons =
                userService.listAvatarIcons();

        return ResponseEntity.ok(icons);
    }

    @PutMapping("/me/avatar")
    @Operation(
            summary = "Alterar ícone de avatar",
            description = """
                    Define o ícone de avatar do usuário autenticado.
                    O valor enviado deve ser um dos ícones retornados
                    por GET /avatar-icons.
                    """,
            operationId = "updateAvatarIcon"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Ícone atualizado com sucesso."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ícone inválido.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado."
            )
    })
    public ResponseEntity<Void> updateAvatarIcon(
            @RequestBody
            @Valid
            UpdateAvatarIconRequest request
    ) {

        Long userId =
                authenticatedUser.getId();

        userService.updateAvatarIcon(
                userId,
                request.icon()
        );

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/favorite-movie")
    @Operation(
            summary = "Definir filme favorito",
            description = """
                    Define o filme favorito do usuário autenticado.
                    Se o filme ainda não existir na base local, ele é
                    buscado e salvo automaticamente a partir do TMDB.
                    """,
            operationId = "updateFavoriteMovie"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Filme favorito atualizado com sucesso."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID de filme inválido.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Filme não encontrado no TMDB.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<Void> updateFavoriteMovie(
            @RequestBody
            @Valid
            UpdateFavoriteMovieRequest request
    ) {

        Long userId =
                authenticatedUser.getId();

        userService.updateFavoriteMovie(
                userId,
                request.movieId()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/favorite-movie")
    @Operation(
            summary = "Remover filme favorito",
            operationId = "removeFavoriteMovie"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Filme favorito removido com sucesso."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado."
            )
    })
    public ResponseEntity<Void> removeFavoriteMovie() {

        Long userId =
                authenticatedUser.getId();

        userService.removeFavoriteMovie(userId);

        return ResponseEntity.noContent().build();
    }
}