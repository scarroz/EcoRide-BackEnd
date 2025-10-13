package co.edu.unbosque.userservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletDTO(Long id,
                        Long userId,
                        BigDecimal balance,
                        LocalDateTime lastUpdated) {
}
