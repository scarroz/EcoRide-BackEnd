package co.edu.unbosque.tripservice.dto;
public record StationStatisticsDTO(
        Long stationId,
        String stationName,
        Integer totalBicycles,
        Integer availableBicycles,
        Integer inUseBicycles,
        Integer reservedBicycles
) {}
