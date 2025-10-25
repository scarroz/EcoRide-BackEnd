package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record RouteSegmentDTO(
        BigDecimal startLat,
        BigDecimal startLon,
        BigDecimal endLat,
        BigDecimal endLon,
        BigDecimal distance,
        Integer duration,
        String roadType
) {}
