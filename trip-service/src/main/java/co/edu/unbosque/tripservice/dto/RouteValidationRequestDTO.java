package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record RouteValidationRequestDTO(
        BigDecimal startLat,
        BigDecimal startLon,
        BigDecimal endLat,
        BigDecimal endLon
) {}
