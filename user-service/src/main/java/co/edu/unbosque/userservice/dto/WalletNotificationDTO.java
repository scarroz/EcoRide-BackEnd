package co.edu.unbosque.userservice.dto;

import java.math.BigDecimal;

public record WalletNotificationDTO(Long userId,
                                    BigDecimal amount) {
}
