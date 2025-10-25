package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripResponseDTO(
        Long id,
        Long userId,
        Long bicycleId,
        String bicycleCode,
        Long startStationId,
        String startStationName,
        Long endStationId,
        String endStationName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal distanceKm,
        BigDecimal totalCost,
        String paymentSource,
        String status,
        String tripType,
        Long durationMinutes
) {
}
