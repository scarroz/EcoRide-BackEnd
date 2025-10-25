package co.edu.unbosque.tripservice.dto;

public record ReservationRequestDTO(
        Long userId,
        Long bicycleId,
        Long stationId
) {
}
