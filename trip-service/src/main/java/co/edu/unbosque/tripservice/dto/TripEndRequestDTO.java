package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record TripEndRequestDTO(
        Long tripId,
        Long endStationId,
        BigDecimal finalLatitude,
        BigDecimal finalLongitude
) {
}
