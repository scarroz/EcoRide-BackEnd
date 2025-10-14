package co.edu.unbosque.userservice.controller;

import co.edu.unbosque.userservice.dto.LoginRequestDTO;
import co.edu.unbosque.userservice.dto.LoginResponseDTO;
import co.edu.unbosque.userservice.service.AuthService;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para autenticacion de usuarios
 * Maneja login y generacion de tokens JWT
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints para autenticacion y generacion de tokens JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Iniciar sesion",
            description = "Autentica un usuario usando email y password. " +
                    "Retorna un token JWT valido para acceder a los servicios protegidos. " +
                    "El token debe incluirse en el header Authorization: Bearer {token}"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login exitoso. Retorna token JWT e informacion del usuario",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                  "message": "Login exitoso",
                                  "userId": 1,
                                  "email": "sebastian@example.com",
                                  "fullName": "Sebastian Carroz"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Credenciales invalidas o datos mal formados",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "error": "Error de autenticacion",
                                  "message": "Credenciales invalidas"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "error": "Usuario no encontrado",
                                  "message": "Usuario no encontrado con email: usuario@example.com"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Cuenta inactiva o bloqueada"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Credenciales de login",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequestDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "email": "sebastian@example.com",
                                  "password": "SecurePass123"
                                }
                                """)
                    )
            )
            LoginRequestDTO request
    ) {
        try {
            LoginResponseDTO response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error de autenticacion");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Usuario no encontrado");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}