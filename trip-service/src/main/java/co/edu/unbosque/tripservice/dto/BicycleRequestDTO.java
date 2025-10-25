package co.edu.unbosque.tripservice.dto;

public record BicycleRequestDTO(
        String code,
        String type,
        Long stationId
) {
}
