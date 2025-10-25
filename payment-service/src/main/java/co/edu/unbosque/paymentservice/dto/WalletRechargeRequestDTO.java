package co.edu.unbosque.paymentservice.dto;

import java.math.BigDecimal;

public record WalletRechargeRequestDTO(
        Long userId,
        BigDecimal amount,
        String paymentMethodId
) {}
