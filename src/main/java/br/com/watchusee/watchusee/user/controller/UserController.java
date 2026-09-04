package br.com.watchusee.watchusee.user.controller;

import br.com.watchusee.watchusee.shared.api.dto.ErrorResponse;
import br.com.watchusee.watchusee.user.api.dto.CreateUserRequest;
import br.com.watchusee.watchusee.user.api.dto.UserResponse;
import br.com.watchusee.watchusee.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(
        name = "Users",
        description = "Operações de cadastro de usuários."
)
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 1,
                                              "nick": "lucas"
                                            }
                                            """
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2026-08-28T03:40:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "A senha deve possuir entre 6 e 100 caracteres.",
                                              "path": "/api/v1/users"
                                            }
                                            """
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2026-08-28T03:40:00Z",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "O nick informado já está em uso.",
                                              "path": "/api/v1/users"
                                            }
                                            """
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
}