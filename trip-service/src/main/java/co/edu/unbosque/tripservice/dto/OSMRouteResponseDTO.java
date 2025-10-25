package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record OSMRouteResponseDTO(
        String status,
        BigDecimal totalDistance,
        Integer totalDuration,
        List<RouteSegmentDTO> segments,
        List<List<BigDecimal>> geometry // coordenadas [lon, lat]
) {}
