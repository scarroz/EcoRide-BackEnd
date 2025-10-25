package co.edu.unbosque.notificationservice.controller;

import co.edu.unbosque.notificationservice.dto.NotificationRequestDTO;
import co.edu.unbosque.notificationservice.dto.NotificationResponseDTO;
import co.edu.unbosque.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones", description = "API para gestión y envío de notificaciones")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "Enviar notificación genérica",
            description = "Envía una notificación basada en una plantilla predefinida a un usuario específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación procesada correctamente",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@Valid @RequestBody NotificationRequestDTO request) {
        try {
            log.info("Solicitud de envío de notificación recibida: userId={}, template={}",
                    request.userId(), request.templateCode());

            NotificationResponseDTO response = notificationService.sendNotification(request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Solicitud inválida: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Solicitud inválida",
                            "message", e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));

        } catch (Exception e) {
            log.error("Error al procesar notificación", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Error interno del servidor",
                            "message", e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    @Operation(
            summary = "Enviar código de recuperación de contraseña",
            description = "Genera y envía un código de 6 dígitos para recuperación de contraseña"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código enviado exitosamente",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error al enviar código")
    })
    @PostMapping("/password-recovery")
    public ResponseEntity<?> sendPasswordRecovery(@RequestBody Map<String, Object> payload) {
        try {
            Integer userId = (Integer) payload.get("userId");
            String code = (String) payload.get("code");

            System.out.println("Solicitud de envío de código de recuperación recibida en notification-service para userId: " + userId);

            NotificationResponseDTO response = notificationService.sendPasswordRecoveryCode(userId, code);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("Error al enviar código de recuperación: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status)
                    .body(Map.of("error", "Error al enviar código de recuperación", "message", e.getMessage()));
        }
    }


}