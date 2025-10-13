package co.edu.unbosque.paymentservice.controller;

import co.edu.unbosque.paymentservice.dto.TransactionDetailDTO;
import co.edu.unbosque.paymentservice.dto.TransactionResponseDTO;
import co.edu.unbosque.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para consultar transacciones de pagos
 * Permite ver el historial completo de recargas, pagos de viajes y suscripciones
 *
 * @author Tu Nombre
 * @version 1.0
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Endpoints para consultar historial de transacciones")
public class TransactionController {

    private final PaymentService paymentService;

    public TransactionController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ========================================
    // CONSULTAS DE TRANSACCIONES
    // ========================================

    @Operation(
            summary = "Obtener todas las transacciones de un usuario",
            description = "Retorna el historial completo de transacciones del usuario ordenadas por fecha " +
                    "(más recientes primero). Incluye recargas de wallet, pagos de viajes y suscripciones."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de transacciones obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Transacciones obtenidas exitosamente",
                                  "count": 5,
                                  "data": [
                                    {
                                      "transactionId": 15,
                                      "userId": 7,
                                      "amount": 25.00,
                                      "type": "TOP_UP",
                                      "source": "CARD",
                                      "status": "COMPLETED",
                                      "stripePaymentId": "pi_3SGq7uDz9DdnuWM72fucSWJM",
                                      "createdAt": "2025-01-10T15:30:45"
                                    },
                                    {
                                      "transactionId": 14,
                                      "userId": 7,
                                      "amount": 12.50,
                                      "type": "TRIP_PAYMENT",
                                      "source": "WALLET",
                                      "status": "COMPLETED",
                                      "stripePaymentId": null,
                                      "createdAt": "2025-01-10T14:20:30"
                                    }
                                  ]
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontraron transacciones para el usuario"
            )
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(
            @Parameter(description = "ID del usuario", required = true, example = "7")
            @PathVariable Long userId
    ) {
        try {
            List<TransactionResponseDTO> transactions = paymentService.getTransactionsByUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transacciones obtenidas exitosamente");
            response.put("count", transactions.size());
            response.put("data", transactions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error obteniendo transacciones: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error obteniendo transacciones");
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(
            summary = "Obtener detalle de una transacción específica",
            description = "Retorna información detallada de una transacción por su ID, " +
                    "incluyendo datos del método de pago utilizado (si aplica)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transacción encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Transacción encontrada",
                                  "data": {
                                    "id": 15,
                                    "userId": 7,
                                    "tripId": null,
                                    "paymentMethodId": 3,
                                    "walletId": null,
                                    "amount": 25.00,
                                    "type": "TOP_UP",
                                    "source": "CARD",
                                    "status": "COMPLETED",
                                    "stripePaymentId": "pi_3SGq7uDz9DdnuWM72fucSWJM",
                                    "createdAt": "2025-01-10T15:30:45",
                                    "cardBrand": "visa",
                                    "cardLast4": "4242"
                                  }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transacción no encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "error": "Error obteniendo transacción",
                                  "message": "Transacción no encontrada"
                                }
                                """)
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(
            @Parameter(description = "ID de la transacción", required = true, example = "15")
            @PathVariable Long id
    ) {
        try {
            TransactionDetailDTO transaction = paymentService.getTransactionDetail(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transacción encontrada");
            response.put("data", transaction);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error obteniendo transacción: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error obteniendo transacción");
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(
            summary = "Filtrar transacciones por estado",
            description = "Obtiene las transacciones de un usuario filtradas por su estado. " +
                    "Estados válidos: PENDING, COMPLETED, FAILED, PROCESSING"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transacciones filtradas exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Transacciones filtradas exitosamente",
                                  "status": "COMPLETED",
                                  "count": 3,
                                  "data": [
                                    {
                                      "transactionId": 15,
                                      "userId": 7,
                                      "amount": 25.00,
                                      "type": "TOP_UP",
                                      "source": "CARD",
                                      "status": "COMPLETED",
                                      "stripePaymentId": "pi_3SGq7uDz9DdnuWM72fucSWJM",
                                      "createdAt": "2025-01-10T15:30:45"
                                    }
                                  ]
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Estado inválido o error en la consulta"
            )
    })
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<?> getTransactionsByStatus(
            @Parameter(description = "ID del usuario", required = true, example = "7")
            @PathVariable Long userId,

            @Parameter(
                    description = "Estado de la transacción",
                    required = true,
                    example = "COMPLETED",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"PENDING", "COMPLETED", "FAILED", "PROCESSING"}
                    )
            )
            @PathVariable String status
    ) {
        try {
            // Validar que el status sea válido
            String normalizedStatus = status.toUpperCase();
            if (!List.of("PENDING", "COMPLETED", "FAILED", "PROCESSING").contains(normalizedStatus)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Estado inválido");
                error.put("message", "Los estados válidos son: PENDING, COMPLETED, FAILED, PROCESSING");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            List<TransactionResponseDTO> transactions = paymentService
                    .getTransactionsByUserAndStatus(userId, normalizedStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transacciones filtradas exitosamente");
            response.put("status", normalizedStatus);
            response.put("count", transactions.size());
            response.put("data", transactions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error filtrando transacciones: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error filtrando transacciones");
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(
            summary = "Obtener estadísticas de transacciones del usuario",
            description = "Retorna un resumen con totales por tipo de transacción y estado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estadísticas calculadas exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "message": "Estadísticas calculadas exitosamente",
                                  "data": {
                                    "totalTransactions": 10,
                                    "totalAmount": 150.00,
                                    "byType": {
                                      "TOP_UP": 3,
                                      "TRIP_PAYMENT": 6,
                                      "SUBSCRIPTION": 1
                                    },
                                    "byStatus": {
                                      "COMPLETED": 9,
                                      "FAILED": 1
                                    }
                                  }
                                }
                                """)
                    )
            )
    })
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<?> getTransactionStats(
            @Parameter(description = "ID del usuario", required = true, example = "7")
            @PathVariable Long userId
    ) {
        try {
            // Obtener todas las transacciones del usuario
            List<TransactionResponseDTO> transactions = paymentService.getTransactionsByUser(userId);

            // Calcular estadísticas
            Map<String, Long> byType = new HashMap<>();
            Map<String, Long> byStatus = new HashMap<>();
            double totalAmount = 0.0;

            for (TransactionResponseDTO t : transactions) {
                // Contar por tipo
                byType.merge(t.type(), 1L, Long::sum);

                // Contar por estado
                byStatus.merge(t.status(), 1L, Long::sum);

                // Sumar montos completados
                if ("COMPLETED".equals(t.status())) {
                    totalAmount += t.amount().doubleValue();
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTransactions", transactions.size());
            stats.put("totalAmount", totalAmount);
            stats.put("byType", byType);
            stats.put("byStatus", byStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Estadísticas calculadas exitosamente");
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error calculando estadísticas: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error calculando estadísticas");
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}