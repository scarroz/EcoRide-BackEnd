package co.edu.unbosque.userservice.dto;

import java.math.BigDecimal;

public record WalletRechargeResponseDTO(String message,
                                        BigDecimal newBalance,
                                        BigDecimal amount,
                                        String transactionId) {
}
