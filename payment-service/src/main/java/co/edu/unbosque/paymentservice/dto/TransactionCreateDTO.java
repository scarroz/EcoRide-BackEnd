package co.edu.unbosque.paymentservice.dto;

import java.math.BigDecimal;

public record TransactionCreateDTO(
        Long userId,
        Long tripId,           // null para recargas
        Long paymentMethodId,
        Long walletId,         // null para recargas con tarjeta
        BigDecimal amount,
        String type,           // TOP_UP, TRIP_PAYMENT, SUBSCRIPTION
        String source          // CARD, WALLET
) {}
