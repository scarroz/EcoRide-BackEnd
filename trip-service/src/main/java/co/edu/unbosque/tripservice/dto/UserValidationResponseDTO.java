package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record UserValidationResponseDTO(
        Boolean valid,
        String status,
        BigDecimal walletBalance
) {}
