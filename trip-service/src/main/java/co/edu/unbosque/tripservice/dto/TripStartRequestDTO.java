package co.edu.unbosque.tripservice.dto;

public record TripStartRequestDTO(
        Long userId,
        Long bicycleId,
        Long stationId,
        String tripType,
        String paymentSource
) {
}
