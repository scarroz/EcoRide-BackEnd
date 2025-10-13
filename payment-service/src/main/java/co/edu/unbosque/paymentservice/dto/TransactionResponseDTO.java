package co.edu.unbosque.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDTO(
        Long transactionId,
        Long userId,
        BigDecimal amount,
        String type,
        String source,
        String status,
        String stripePaymentId,
        LocalDateTime createdAt
) {}
