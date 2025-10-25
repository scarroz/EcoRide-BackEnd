package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripDetailDTO(
        Long id,
        Long userId,
        String userName,
        BicycleResponseDTO bicycle,
        StationResponseDTO startStation,
        StationResponseDTO endStation,
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
