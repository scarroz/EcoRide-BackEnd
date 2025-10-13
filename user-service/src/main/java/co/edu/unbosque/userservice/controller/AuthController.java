package co.edu.unbosque.userservice.controller;

import co.edu.unbosque.userservice.dto.LoginRequestDTO;
import co.edu.unbosque.userservice.dto.LoginResponseDTO;
import co.edu.unbosque.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticación", description = "Gestión de inicio de sesión y generación de tokens JWT para los usuarios de EcoRide")
@RestController
@RequestMapping("/api/users")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(
            summary = "Iniciar sesión",
            description = "Permite que un usuario inicie sesión en EcoRide usando su correo y contraseña. Retorna un token JWT válido para acceder a los demás servicios.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso. Devuelve el token JWT.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = LoginResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Credenciales inválidas o datos mal enviados.",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "Usuario no autorizado."),
                    @ApiResponse(responseCode = "500", description = "Error interno en el servidor.")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
