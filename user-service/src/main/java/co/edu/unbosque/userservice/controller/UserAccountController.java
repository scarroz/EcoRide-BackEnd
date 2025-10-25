package co.edu.unbosque.userservice.controller;

import co.edu.unbosque.userservice.dto.*;
import co.edu.unbosque.userservice.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestion de cuentas de usuario
 * Maneja registro, actualizacion, consulta y eliminacion de usuarios
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Endpoints para gestion de cuentas de usuario")
public class UserAccountController {

    private final UserAccountService userService;

    public UserAccountController(UserAccountService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Crea una nueva cuenta de usuario con wallet asociada. " +
                    "El password se encripta automaticamente. La wallet inicia con saldo $0."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario registrado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountResponseDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Usuario registrado exitosamente",
                                  "user": {
                                    "id": 1,
                                    "fullName": "Sebastian Carroz",
                                    "email": "sebastian@example.com",
                                    "documentNumber": "1234567890",
                                    "status": "ACTIVE",
                                    "verified": false,
                                    "walletBalance": 0.00,
                                    "createdAt": "2025-01-10T15:30:45"
                                  }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email o documento ya registrado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "error": "Error al registrar usuario",
                                  "message": "El email ya esta registrado"
                                }
                                """)
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del nuevo usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserAccountRequestDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "fullName": "Sebastian Carroz",
                                  "documentNumber": "1234567890",
                                  "email": "sebastian@example.com",
                                  "password": "SecurePass123"
                                }
                                """)
                    )
            )
            UserAccountRequestDTO dto
    ) {
        try {
            UserAccountResponseDTO user = userService.registerUser(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Usuario registrado exitosamente");
            response.put("user", user);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al registrar usuario");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(
            summary = "Listar todos los usuarios",
            description = "Obtiene una lista completa de todos los usuarios registrados con sus wallets"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de usuarios obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Usuarios obtenidos exitosamente",
                                  "count": 2,
                                  "users": [
                                    {
                                      "id": 1,
                                      "fullName": "Sebastian Carroz",
                                      "email": "sebastian@example.com",
                                      "documentNumber": "1234567890",
                                      "status": "ACTIVE",
                                      "verified": false,
                                      "walletBalance": 50.00,
                                      "createdAt": "2025-01-10T15:30:45"
                                    }
                                  ]
                                }
                                """)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<?> listAll() {
        try {
            List<UserAccountResponseDTO> users = userService.listAllUsers();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Usuarios obtenidos exitosamente");
            response.put("count", users.size());
            response.put("users", users);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al obtener usuarios");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(
            summary = "Obtener usuario por ID",
            description = "Obtiene informacion basica de un usuario y su wallet por ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "id": 1,
                                  "fullName": "Sebastian Carroz",
                                  "email": "sebastian@example.com",
                                  "documentNumber": "1234567890",
                                  "status": "ACTIVE",
                                  "verified": false,
                                  "walletBalance": 50.00,
                                  "createdAt": "2025-01-10T15:30:45"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long id
    ) {
        try {
            UserAccountResponseDTO user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Usuario no encontrado");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(
            summary = "Obtener detalle completo del usuario",
            description = "Obtiene informacion detallada del usuario incluyendo perfil y wallet"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle del usuario obtenido",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "id": 1,
                                  "fullName": "Sebastian Carroz",
                                  "email": "sebastian@example.com",
                                  "documentNumber": "1234567890",
                                  "status": "ACTIVE",
                                  "verified": false,
                                  "createdAt": "2025-01-10T15:30:45",
                                  "profile": {
                                    "id": 1,
                                    "phone": "+57 300 1234567",
                                    "address": "Calle 123 #45-67",
                                    "city": "Bogota",
                                    "country": "Colombia"
                                  },
                                  "wallet": {
                                    "id": 1,
                                    "userId": 1,
                                    "balance": 50.00,
                                    "lastUpdated": "2025-01-10T16:00:00"
                                  }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @GetMapping("/{id}/detail")
    public ResponseEntity<?> getUserDetail(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long id
    ) {
        try {
            UserAccountDetailDTO detail = userService.getUserDetailById(id);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Usuario no encontrado");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(
            summary = "Actualizar usuario",
            description = "Actualiza la informacion basica del usuario. " +
                    "Los campos no enviados no se modifican."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario actualizado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Usuario actualizado exitosamente",
                                  "user": {
                                    "id": 1,
                                    "fullName": "Sebastian Carroz Actualizado",
                                    "email": "nuevo@example.com",
                                    "documentNumber": "1234567890",
                                    "status": "ACTIVE",
                                    "verified": false,
                                    "walletBalance": 50.00,
                                    "createdAt": "2025-01-10T15:30:45"
                                  }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long id,

            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos a actualizar",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserAccountUpdateDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "fullName": "Sebastian Carroz Actualizado",
                                  "email": "nuevo@example.com",
                                  "password": "NuevoPass123"
                                }
                                """)
                    )
            )
            UserAccountUpdateDTO dto
    ) {
        try {
            UserAccountResponseDTO user = userService.updateUser(id, dto);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Usuario actualizado exitosamente");
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al actualizar usuario");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(
            summary = "Eliminar usuario",
            description = "Elimina permanentemente un usuario y su wallet asociada"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Usuario eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long id
    ) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al eliminar usuario");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(
            summary = "Crear o actualizar perfil de usuario",
            description = "Crea un nuevo perfil o actualiza el existente con informacion adicional del usuario"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil guardado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Perfil actualizado exitosamente",
                                  "profile": {
                                    "id": 1,
                                    "phone": "+57 300 1234567",
                                    "address": "Calle 123 #45-67",
                                    "city": "Bogota",
                                    "country": "Colombia"
                                  }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> createOrUpdateProfile(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long userId,

            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del perfil",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserProfileRequestDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "phone": "+57 300 1234567",
                                  "address": "Calle 123 #45-67",
                                  "city": "Bogota",
                                  "country": "Colombia"
                                }
                                """)
                    )
            )
            UserProfileRequestDTO request
    ) {
        try {
            UserProfileDTO profile = userService.createOrUpdateProfile(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Perfil actualizado exitosamente");
            response.put("profile", profile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al guardar perfil");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(
            summary = "Obtener perfil de usuario",
            description = "Obtiene el perfil completo del usuario"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil obtenido exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Perfil no encontrado"
            )
    })
    @GetMapping("/{userId}/profile")
    public ResponseEntity<?> getUserProfile(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long userId
    ) {
        try {
            UserProfileDTO profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Perfil no encontrado");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }



    @Operation(
            summary = "Obtener email del usuario por ID",
            description = "Devuelve el email asociado al ID del usuario. Usado por el servicio de notificaciones."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Email obtenido exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @GetMapping("/{id}/email")
    public ResponseEntity<?> getUserEmailById(@PathVariable Long id) {
        try {
            var user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado", "message", "No existe el usuario con ID " + id));
            }
    System.out.println("Estamos aca");
            // DTO que espera notification-service
            var emailDto = new UserEmailDTO(user.id().intValue(), user.fullName(), user.email());
            return ResponseEntity.ok(emailDto);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener email", "message", e.getMessage()));
        }



    }


    @Operation(summary = "Restablecer contraseña con código")
    @PostMapping("/password-recovery/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDTO dto) {
        try {
            String message = userService.resetPassword(dto.email(), dto.code(), dto.newPassword());
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error", "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/password-recovery")
    public ResponseEntity<?> requestPasswordRecovery(@PathVariable("id") Long id) {
        try {
            System.out.println("Solicitud de código de recuperación recibida para userId: " + id);
            String result = userService.requestPasswordRecovery(id);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (RuntimeException e) {
            System.out.println("Error al solicitar código de recuperación: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}





