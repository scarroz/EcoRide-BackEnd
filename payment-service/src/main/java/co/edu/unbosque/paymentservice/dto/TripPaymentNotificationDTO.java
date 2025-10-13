package co.edu.unbosque.paymentservice.dto;

import java.math.BigDecimal;

public record TripPaymentNotificationDTO(
        Long tripId,
        Long userId,
        BigDecimal amount,
        String transactionId
) {}
