package co.edu.unbosque.tripservice.dto;

public record BicycleResponseDTO(
        Long id,
        String code,
        String type,
        Integer batteryLevel,
        String status,
        Long lastStationId,
        String lastStationName
) {
}
