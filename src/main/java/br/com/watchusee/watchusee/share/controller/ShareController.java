package br.com.watchusee.watchusee.share.controller;

import br.com.watchusee.watchusee.share.api.dto.CreateShareRequest;
import br.com.watchusee.watchusee.share.api.dto.ShareResponse;
import br.com.watchusee.watchusee.share.service.ShareService;
import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shares")
@Tag(
        name = "Shares",
        description = "Compartilhamento de filmes entre usuários."
)
public class ShareController {

    private final ShareService shareService;
    private final AuthenticatedUser authenticatedUser;

    public ShareController(
            ShareService shareService,
            AuthenticatedUser authenticatedUser
    ) {
        this.shareService = shareService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    @Operation(
            summary = "Compartilhar filme com outro usuário"
    )
    public ResponseEntity<ShareResponse> createShare(

            @Valid
            @RequestBody
            CreateShareRequest request

    ) {

        Long userId =
                authenticatedUser.getId();

        ShareResponse response =
                shareService.createShare(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/received")
    @Operation(
            summary = "Listar compartilhamentos recebidos"
    )
    public ResponseEntity<List<ShareResponse>> getReceivedShares() {

        Long userId =
                authenticatedUser.getId();

        return ResponseEntity.ok(
                shareService.getReceivedShares(userId)
        );
    }

    @GetMapping("/pending")
    @Operation(
            summary = "Listar compartilhamentos pendentes"
    )
    public ResponseEntity<List<ShareResponse>> getPendingShares() {

        Long userId =
                authenticatedUser.getId();

        return ResponseEntity.ok(
                shareService.getPendingShares(userId)
        );
    }

    @GetMapping("/sent")
    @Operation(
            summary = "Listar compartilhamentos enviados"
    )
    public ResponseEntity<List<ShareResponse>> getSentShares() {

        Long userId =
                authenticatedUser.getId();

        return ResponseEntity.ok(
                shareService.getSentShares(userId)
        );
    }

    @PatchMapping("/{shareId}/accept")
    @Operation(
            summary = "Aceitar compartilhamento"
    )
    public ResponseEntity<ShareResponse> acceptShare(

            @PathVariable
            @Positive
            Long shareId

    ) {

        Long userId =
                authenticatedUser.getId();

        ShareResponse response =
                shareService.acceptShare(
                        userId,
                        shareId
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shareId}/reject")
    @Operation(
            summary = "Recusar compartilhamento"
    )
    public ResponseEntity<ShareResponse> rejectShare(

            @PathVariable
            @Positive
            Long shareId

    ) {

        Long userId =
                authenticatedUser.getId();

        ShareResponse response =
                shareService.rejectShare(
                        userId,
                        shareId
                );

        return ResponseEntity.ok(response);
    }
}