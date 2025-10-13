package co.edu.unbosque.paymentservice.dto;

import java.math.BigDecimal;

public record TripPaymentRequestDTO(
        Long userId,
        Long tripId,
        BigDecimal amount
) {}
