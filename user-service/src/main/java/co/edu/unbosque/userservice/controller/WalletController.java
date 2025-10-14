package co.edu.unbosque.userservice.controller;

import co.edu.unbosque.userservice.dto.*;
import co.edu.unbosque.userservice.service.WalletService;
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
import java.util.Map;

/**
 * Controlador REST para gestion de wallets y recargas
 * Maneja el registro de tarjetas y recargas de saldo
 */
@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Wallet", description = "Endpoints para gestion de wallets y recargas")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
            summary = "Registrar metodo de pago",
            description = "Registra una tarjeta tokenizada por Stripe en el Payment Service. " +
                    "El token debe generarse en el frontend usando Stripe.js. " +
                    "Esta operacion es PCI-DSS compliant."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tarjeta registrada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Tarjeta registrada correctamente en Payment Service",
                                  "userId": 1
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error al registrar tarjeta",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "error": "Error al registrar tarjeta",
                                  "message": "Token de Stripe invalido"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @PostMapping("/register-card")
    public ResponseEntity<?> registerCard(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos de la tarjeta tokenizada",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PaymentCardRequestDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "userId": 1,
                                  "name": "Sebastian Carroz",
                                  "email": "sebastian@example.com",
                                  "paymentMethodId": "pm_card_visa"
                                }
                                """)
                    )
            )
            PaymentCardRequestDTO request
    ) {
        try {
            walletService.registerCard(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tarjeta registrada correctamente en Payment Service");
            response.put("userId", request.userId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al registrar tarjeta");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(
            summary = "Recargar wallet",
            description = "Procesa una recarga de saldo en la wallet del usuario usando un metodo de pago registrado. " +
                    "La transaccion se procesa en el Payment Service con Stripe. " +
                    "El saldo se actualiza automaticamente cuando el pago es confirmado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Recarga procesada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Recarga en proceso. El saldo se actualizara una vez confirmado el pago.",
                                  "newBalance": 50.00,
                                  "amount": 25.00,
                                  "transactionId": "pending"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error al procesar recarga",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "error": "Error al procesar recarga",
                                  "message": "El monto minimo de recarga es $1.00"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario o wallet no encontrada"
            )
    })
    @PostMapping("/recharge")
    public ResponseEntity<?> rechargeWallet(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos de la recarga",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = WalletRechargeRequestDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "userId": 1,
                                  "amount": 25.00,
                                  "paymentMethodId": "pm_1SGpxaDz9DdnuWM7bx09n9dQ"
                                }
                                """)
                    )
            )
            WalletRechargeRequestDTO request
    ) {
        try {
            WalletRechargeResponseDTO response = walletService.rechargeWallet(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Datos invalidos");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al procesar recarga");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(
            summary = "Notificacion de recarga (interno)",
            description = "Endpoint interno usado por el Payment Service para notificar cuando una recarga fue confirmada. " +
                    "Actualiza el saldo de la wallet automaticamente. " +
                    "NO debe ser llamado directamente por clientes externos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Saldo actualizado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet no encontrada"
            )
    })
    @PostMapping("/notify-recharge")
    public ResponseEntity<?> notifyRecharge(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Notificacion de recarga confirmada",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = WalletNotificationDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "userId": 1,
                                  "amount": 25.00
                                }
                                """)
                    )
            )
            WalletNotificationDTO notification
    ) {
        try {
            walletService.updateWalletBalance(notification);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Saldo actualizado correctamente");
            response.put("userId", notification.userId());
            response.put("amount", notification.amount());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al actualizar saldo");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(
            summary = "Obtener wallet por ID de usuario",
            description = "Consulta la informacion de la wallet del usuario incluyendo su saldo actual"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "id": 1,
                                  "userId": 1,
                                  "balance": 50.00,
                                  "lastUpdated": "2025-01-10T16:00:00"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet no encontrada"
            )
    })
    @GetMapping("/{userId}")
    public ResponseEntity<?> getWallet(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long userId
    ) {
        try {
            WalletDTO wallet = walletService.getWalletByUserId(userId);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Wallet no encontrada");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}