package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record RouteValidationResponseDTO(
        Boolean valid,
        BigDecimal distanceKm,
        Long estimatedDurationMinutes,
        String message
) {}