package co.edu.unbosque.tripservice.dto;

import java.time.LocalDateTime;

public record ReservationResponseDTO(
        Long id,
        Long userId,
        Long bicycleId,
        String bicycleCode,
        Long stationId,
        String stationName,
        LocalDateTime reservedAt,
        LocalDateTime expiresAt,
        String status
) {
}
