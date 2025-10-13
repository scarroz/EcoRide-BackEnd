package co.edu.unbosque.userservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserAccountResponseDTO(
        Long id,
        String fullName,
        String email,
        String documentNumber,
        String status,
        boolean verified,
        BigDecimal walletBalance,
        LocalDateTime createdAt
) {}
