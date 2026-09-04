package br.com.watchusee.watchusee.user.controller;

import br.com.watchusee.watchusee.shared.api.dto.ErrorResponse;
import br.com.watchusee.watchusee.user.api.dto.LoginRequest;
import br.com.watchusee.watchusee.user.api.dto.LoginResponse;
import br.com.watchusee.watchusee.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
        name = "Authentication",
        description = "Operações de autenticação."
)
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Realizar login",
            description = """
                    Autentica um usuário utilizando nick e senha.

                    Em caso de sucesso, retorna um JWT que deverá
                    ser enviado nas próximas requisições protegidas
                    através do header:

                    Authorization: Bearer <token>
                    """,
            operationId = "login"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login realizado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = LoginResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 1,
                                              "nick": "lucas",
                                              "token": "eyJhbGciOiJIUzI1NiJ9..."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de login inválidos.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Nick ou senha inválidos.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2026-08-28T03:45:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Nick ou senha inválidos.",
                                              "path": "/api/v1/auth/login"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<LoginResponse> login(

            @RequestBody
            @Valid
            LoginRequest request

    ) {

        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }
}