package br.com.watchusee.watchusee.user.controller;

import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import br.com.watchusee.watchusee.user.api.dto.UserProfileResponse;
import br.com.watchusee.watchusee.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(
        name = "User Profile",
        description = "Informações do perfil do usuário autenticado."
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
}