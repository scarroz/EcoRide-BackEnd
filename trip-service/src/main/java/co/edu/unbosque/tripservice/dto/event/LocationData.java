package co.edu.unbosque.tripservice.dto.event;

import java.math.BigDecimal;

public record LocationData(
        BigDecimal latitude,
        BigDecimal longitude
) {}
