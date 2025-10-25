package co.edu.unbosque.tripservice.dto;

public record BicycleStatusUpdateDTO(
        String status,
        Integer batteryLevel,
        Long stationId
) {
}
