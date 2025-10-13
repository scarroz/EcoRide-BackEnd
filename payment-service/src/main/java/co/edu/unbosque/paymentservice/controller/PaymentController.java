package co.edu.unbosque.paymentservice.controller;

import co.edu.unbosque.paymentservice.dto.*;
import co.edu.unbosque.paymentservice.service.PaymentService;
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
 * Controlador REST para gestionar pagos, métodos de pago y transacciones
 * Integrado con Stripe para procesamiento de pagos
 *
 * @author SEBASTIAN ERNESTO CARROZ AÑEZ
 * @version 1.0
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints para gestión de pagos y métodos de pago con Stripe")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ========================================
    // MÉTODOS DE PAGO (TARJETAS)
    // ========================================

    @Operation(
            summary = "Registrar tarjeta de crédito/débito",
            description = "Registra un nuevo método de pago (tarjeta) tokenizado por Stripe. " +
                    "El frontend debe enviar el token generado por Stripe.js (pm_xxx), " +
                    "NUNCA los datos reales de la tarjeta. Este endpoint es PCI-DSS compliant."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tarjeta registrada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentMethodResponseDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Tarjeta registrada correctamente",
                                  "paymentMethod": {
                                    "paymentMethodId": "pm_1SGpxaDz9DdnuWM7bx09n9dQ",
                                    "brand": "visa",
                                    "last4": "4242",
                                    "isDefault": false
                                  }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en la validación o al procesar con Stripe",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "error": "No se pudo registrar la tarjeta",
                                  "details": "El método de pago no es válido"
                                }
                                """)
                    )
            )
    })
    @PostMapping("/cards/register")
    public ResponseEntity<?> registerCard(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos para registrar la tarjeta (token de Stripe)",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PaymentCardRequestDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "userId": 7,
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
            PaymentMethodResponseDTO response = paymentService.registerCard(request);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Tarjeta registrada correctamente");
            result.put("paymentMethod", response);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            System.err.println("Error registrando tarjeta: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "No se pudo registrar la tarjeta");
            error.put("details", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(
            summary = "Listar métodos de pago del usuario",
            description = "Obtiene todos los métodos de pago activos asociados al usuario. " +
                    "Retorna solo tokens y últimos 4 dígitos, nunca datos completos de tarjeta."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de métodos de pago obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Métodos de pago obtenidos exitosamente",
                                  "count": 2,
                                  "data": [
                                    {
                                      "paymentMethodId": "pm_1SGpxaDz9DdnuWM7bx09n9dQ",
                                      "brand": "visa",
                                      "last4": "4242",
                                      "isDefault": true
                                    },
                                    {
                                      "paymentMethodId": "pm_1SGpxbDz9DdnuWM7cY10n9eR",
                                      "brand": "mastercard",
                                      "last4": "5555",
                                      "isDefault": false
                                    }
                                  ]
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontraron métodos de pago para el usuario"
            )
    })
    @GetMapping("/methods/{userId}")
    public ResponseEntity<?> getUserPaymentMethods(
            @Parameter(description = "ID del usuario", required = true, example = "7")
            @PathVariable Long userId
    ) {
        try {
            List<PaymentMethodResponseDTO> methods = paymentService.getActiveMethodsByUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Métodos de pago obtenidos exitosamente");
            response.put("count", methods.size());
            response.put("data", methods);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error obteniendo métodos de pago: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al obtener los métodos de pago");
            error.put("details", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ========================================
    // RECARGAS DE WALLET
    // ========================================

    @Operation(
            summary = "Recargar saldo de wallet",
            description = "Procesa una recarga de wallet usando un método de pago previamente registrado. " +
                    "El pago se procesa con Stripe y, si es exitoso, se notifica al User Service " +
                    "para actualizar el saldo de la wallet. La transacción queda registrada en la base de datos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Recarga procesada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Recarga procesada correctamente",
                                  "transaction": {
                                    "transactionId": 15,
                                    "userId": 7,
                                    "amount": 25.00,
                                    "type": "TOP_UP",
                                    "source": "CARD",
                                    "status": "COMPLETED",
                                    "stripePaymentId": "pi_3SGq7uDz9DdnuWM72fucSWJM",
                                    "createdAt": "2025-01-10T15:30:45"
                                  }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error procesando el pago (fondos insuficientes, tarjeta rechazada, etc.)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "error": "Error procesando la recarga",
                                  "details": "La tarjeta fue rechazada"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Método de pago no encontrado o inactivo"
            )
    })
    @PostMapping("/wallet/recharge")
    public ResponseEntity<?> rechargeWallet(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos para la recarga de wallet",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = WalletRechargeRequestDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                  "userId": 7,
                                  "amount": 25.00,
                                  "paymentMethodId": "pm_1SGpxaDz9DdnuWM7bx09n9dQ"
                                }
                                """)
                    )
            )
            WalletRechargeRequestDTO request
    ) {
        try {
            TransactionResponseDTO transaction = paymentService.rechargeWallet(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Recarga procesada correctamente");
            response.put("transaction", transaction);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error procesando recarga: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error procesando la recarga");
            error.put("details", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}