package br.com.watchusee.watchusee.friend.controller;

import br.com.watchusee.watchusee.friend.api.dto.FriendRequestResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendshipStatusResponse;
import br.com.watchusee.watchusee.friend.service.FriendService;
import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/friends")
@Tag(
        name = "Friends",
        description = "Operações de amizade entre usuários."
)
public class FriendController {

    private final FriendService friendService;
    private final AuthenticatedUser authenticatedUser;

    public FriendController(
            FriendService friendService,
            AuthenticatedUser authenticatedUser
    ) {
        this.friendService = friendService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping("/requests/{userId}")
    @Operation(
            summary = "Enviar solicitação de amizade"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Solicitação enviada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Usuário inválido."
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Solicitação já existente."
            )
    })
    public ResponseEntity<Void> sendRequest(
            @PathVariable @Positive Long userId
    ) {

        friendService.sendRequest(
                authenticatedUser.getId(),
                userId
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests")
    @Operation(
            summary = "Listar solicitações recebidas"
    )
    public ResponseEntity<List<FriendRequestResponse>> getRequests() {

        List<FriendRequestResponse> requests =
                friendService.getReceivedRequests(
                        authenticatedUser.getId()
                );

        return ResponseEntity.ok(requests);
    }

    @PostMapping("/requests/{requestId}/accept")
    @Operation(
            summary = "Aceitar solicitação de amizade"
    )
    public ResponseEntity<Void> acceptRequest(
            @PathVariable @Positive Long requestId
    ) {

        friendService.acceptRequest(
                authenticatedUser.getId(),
                requestId
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/reject")
    @Operation(
            summary = "Recusar solicitação de amizade"
    )
    public ResponseEntity<Void> rejectRequest(
            @PathVariable @Positive Long requestId
    ) {

        friendService.rejectRequest(
                authenticatedUser.getId(),
                requestId
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Listar amigos"
    )
    public ResponseEntity<List<FriendResponse>> getFriends() {

        List<FriendResponse> friends =
                friendService.getFriends(
                        authenticatedUser.getId()
                );

        return ResponseEntity.ok(friends);
    }

    @GetMapping("/count")
    @Operation(
            summary = "Consultar quantidade de amigos"
    )
    public ResponseEntity<Long> countFriends() {

        long count =
                friendService.countFriends(
                        authenticatedUser.getId()
                );

        return ResponseEntity.ok(count);
    }

    @GetMapping("/status/{userId}")
    @Operation(
            summary = "Consultar status de amizade com um usuário",
            description = """
                    Retorna o relacionamento entre o usuário autenticado e o
                    usuário informado: SELF, NONE, FRIENDS, REQUEST_SENT,
                    REQUEST_RECEIVED ou REJECTED.

                    Útil para decidir, na tela de perfil público, se deve
                    ser exibido o botão "Adicionar amigo", "Solicitação
                    enviada", "Aceitar solicitação" ou "Amigos".

                    Quando o status for REQUEST_RECEIVED, o campo
                    friendshipId pode ser usado diretamente nos endpoints
                    POST /friends/requests/{requestId}/accept ou /reject.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retornado com sucesso."
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
    public ResponseEntity<FriendshipStatusResponse> getRelationshipStatus(
            @PathVariable @Positive Long userId
    ) {

        FriendshipStatusResponse response =
                friendService.getRelationshipStatus(
                        authenticatedUser.getId(),
                        userId
                );

        return ResponseEntity.ok(response);
    }
}