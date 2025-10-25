package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record StationRequestDTO(
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer capacity
) {}
