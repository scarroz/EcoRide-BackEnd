package co.edu.unbosque.tripservice.dto;

import java.time.LocalDateTime;

public record LockStatusDTO(
        Long bicycleId,
        String status,
        LocalDateTime timestamp
) {
}
