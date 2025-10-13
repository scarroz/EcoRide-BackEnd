package co.edu.unbosque.userservice.dto;

import java.math.BigDecimal;

public record WalletRechargeRequestDTO(
        Long userId,
        BigDecimal amount,
        String paymentMethodId
) {}
