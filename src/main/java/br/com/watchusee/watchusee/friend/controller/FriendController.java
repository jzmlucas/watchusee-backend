package br.com.watchusee.watchusee.friend.controller;

import br.com.watchusee.watchusee.friend.api.dto.FriendRequestResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendResponse;
import br.com.watchusee.watchusee.friend.api.dto.FriendshipStatusResponse;
import br.com.watchusee.watchusee.friend.service.FriendService;
import br.com.watchusee.watchusee.shared.api.dto.PageResponse;
import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Enviar solicitação de amizade")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação enviada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Usuário inválido ou solicitação para si mesmo."),
            @ApiResponse(responseCode = "404", description = "Usuário destinatário não encontrado."),
            @ApiResponse(responseCode = "409", description = "Já existe uma relação entre os usuários.")
    })
    public ResponseEntity<FriendRequestResponse> sendRequest(
            @PathVariable @Positive Long userId
    ) {
        FriendRequestResponse response =
                friendService.sendRequest(authenticatedUser.getId(), userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/requests/{requestId}/accept")
    @Operation(summary = "Aceitar solicitação de amizade recebida")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitação aceita."),
            @ApiResponse(responseCode = "403", description = "Usuário não é o destinatário da solicitação."),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada."),
            @ApiResponse(responseCode = "409", description = "Solicitação não está mais pendente.")
    })
    public ResponseEntity<Void> acceptRequest(
            @PathVariable @Positive Long requestId
    ) {
        friendService.acceptRequest(authenticatedUser.getId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/reject")
    @Operation(summary = "Recusar solicitação de amizade recebida")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitação recusada."),
            @ApiResponse(responseCode = "403", description = "Usuário não é o destinatário da solicitação."),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada."),
            @ApiResponse(responseCode = "409", description = "Solicitação não está mais pendente.")
    })
    public ResponseEntity<Void> rejectRequest(
            @PathVariable @Positive Long requestId
    ) {
        friendService.rejectRequest(authenticatedUser.getId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/cancel")
    @Operation(summary = "Cancelar solicitação de amizade enviada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitação cancelada."),
            @ApiResponse(responseCode = "403", description = "Usuário não é quem enviou a solicitação."),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada."),
            @ApiResponse(responseCode = "409", description = "Solicitação não está mais pendente.")
    })
    public ResponseEntity<Void> cancelRequest(
            @PathVariable @Positive Long requestId
    ) {
        friendService.cancelRequest(authenticatedUser.getId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remover um amigo (desfazer amizade)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Amizade removida."),
            @ApiResponse(responseCode = "404", description = "Amizade não encontrada.")
    })
    public ResponseEntity<Void> removeFriend(
            @PathVariable @Positive Long userId
    ) {
        friendService.removeFriend(authenticatedUser.getId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Listar amigos do usuário autenticado, paginado")
    public ResponseEntity<PageResponse<FriendResponse>> getFriends(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "respondedAt"));

        PageResponse<FriendResponse> response =
                PageResponse.from(friendService.getFriends(authenticatedUser.getId(), pageable));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests")
    @Operation(
            summary = "Listar solicitações de amizade recebidas (pendentes), paginado"
    )
    public ResponseEntity<PageResponse<FriendRequestResponse>> getReceivedRequests(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        PageResponse<FriendRequestResponse> response =
                PageResponse.from(friendService.getReceivedRequests(authenticatedUser.getId(), pageable));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests/sent")
    @Operation(
            summary = "Listar solicitações de amizade enviadas (pendentes), paginado"
    )
    public ResponseEntity<PageResponse<FriendRequestResponse>> getSentRequests(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        PageResponse<FriendRequestResponse> response =
                PageResponse.from(friendService.getSentRequests(authenticatedUser.getId(), pageable));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    @Operation(summary = "Consultar quantidade de amigos")
    public ResponseEntity<Long> countFriends() {
        long count = friendService.countFriends(authenticatedUser.getId());
        return ResponseEntity.ok(count);
    }

    @GetMapping("/status/{userId}")
    @Operation(
            summary = "Consultar status de amizade com um usuário",
            description = """
                    Retorna o relacionamento entre o usuário autenticado e o
                    usuário informado: SELF, NONE, FRIENDS, REQUEST_SENT,
                    REQUEST_RECEIVED, REJECTED ou CANCELLED.

                    Útil para decidir, na tela de perfil público, se deve
                    ser exibido o botão "Adicionar amigo", "Solicitação
                    enviada", "Aceitar solicitação" ou "Amigos".

                    Quando o status for REQUEST_RECEIVED, o campo
                    friendshipId pode ser usado diretamente nos endpoints
                    POST /friends/requests/{requestId}/accept ou /reject.
                    Quando for REQUEST_SENT, o mesmo id pode ser usado em
                    POST /friends/requests/{requestId}/cancel.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status retornado com sucesso."),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado."),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public ResponseEntity<FriendshipStatusResponse> getRelationshipStatus(
            @PathVariable @Positive Long userId
    ) {
        FriendshipStatusResponse response =
                friendService.getRelationshipStatus(authenticatedUser.getId(), userId);

        return ResponseEntity.ok(response);
    }
}
