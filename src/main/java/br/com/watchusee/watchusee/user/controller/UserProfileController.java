package br.com.watchusee.watchusee.user.controller;

import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import br.com.watchusee.watchusee.user.api.dto.UserProfileResponse;
import br.com.watchusee.watchusee.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
@Tag(
        name = "User Profile",
        description = "Informações de perfil do usuário autenticado e de outros usuários (perfil público)."
)
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthenticatedUser authenticatedUser;

    public UserProfileController(
            UserProfileService userProfileService,
            AuthenticatedUser authenticatedUser
    ) {
        this.userProfileService = userProfileService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/me/profile")
    @Operation(
            summary = "Consultar perfil do usuário autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil retornado com sucesso."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado."
            )
    })
    public ResponseEntity<UserProfileResponse> getProfile() {

        Long userId =
                authenticatedUser.getId();

        UserProfileResponse response =
                userProfileService.getProfile(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    @Operation(
            summary = "Consultar perfil público de um usuário",
            description = """
                    Retorna o perfil público de qualquer usuário cadastrado
                    (id, nick, data de criação da conta, quantidade de filmes
                    assistidos, quantidade de filmes na watchlist e quantidade
                    de amigos).

                    Requer apenas que o requisitante esteja autenticado — não
                    é necessário ser amigo do usuário consultado. Pensado para
                    o fluxo de busca de usuários seguido de envio de pedido
                    de amizade.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil retornado com sucesso."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado."
            )
    })
    public ResponseEntity<UserProfileResponse> getPublicProfile(
            @Parameter(description = "ID do usuário cujo perfil será consultado")
            @PathVariable @Positive Long userId
    ) {

        UserProfileResponse response =
                userProfileService.getProfile(userId);

        return ResponseEntity.ok(response);
    }
}
