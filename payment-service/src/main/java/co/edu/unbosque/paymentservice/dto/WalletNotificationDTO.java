package co.edu.unbosque.paymentservice.dto;

import java.math.BigDecimal;

public record WalletNotificationDTO(
        Long userId,
        BigDecimal amount
) {}
