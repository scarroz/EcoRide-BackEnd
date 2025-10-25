package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record BikeIoTDataDTO(
        Long bikeId,
        String timestamp,
        BigDecimal lat,
        BigDecimal lon,
        Integer battery,
        String lockStatus
)  {
}
