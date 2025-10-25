package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record TripPaymentRequestDTO(
        Long userId,
        Long tripId,
        BigDecimal amount,
        String paymentSource
) {
}
