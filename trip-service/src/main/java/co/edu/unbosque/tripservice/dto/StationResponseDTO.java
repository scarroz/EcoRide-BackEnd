package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record StationResponseDTO(
        Long id,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer capacity,
        Boolean active,
        Integer availableBicycles
) {
}
