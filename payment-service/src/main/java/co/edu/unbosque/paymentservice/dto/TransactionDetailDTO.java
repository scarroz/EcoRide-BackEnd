package co.edu.unbosque.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDetailDTO(
        Long id,
        Long userId,
        Long tripId,
        Long paymentMethodId,
        Long walletId,
        BigDecimal amount,
        String type,
        String source,
        String status,
        String stripePaymentId,
        LocalDateTime createdAt,
        // Información adicional
        String cardBrand,      // Si fue con tarjeta
        String cardLast4
) {}