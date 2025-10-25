package co.edu.unbosque.tripservice.service;

import co.edu.unbosque.tripservice.dto.RouteValidationResponseDTO;

import java.math.BigDecimal;

public interface RouteValidationService {

    /**
     * Valida que la posición final esté dentro del radio de proximidad de la estación.
     *
     * @param stationLat latitud de la estación
     * @param stationLon longitud de la estación
     * @param finalLat   latitud final reportada por la bicicleta
     * @param finalLon   longitud final reportada por la bicicleta
     * @return resultado de la validación, incluyendo distancia y mensaje
     */
    RouteValidationResponseDTO validateFinalPosition(
            BigDecimal stationLat,
            BigDecimal stationLon,
            BigDecimal finalLat,
            BigDecimal finalLon
    );

    /**
     * Calcula la distancia total de un viaje y valida que esté dentro de los límites de Bogotá.
     *
     * @param startLat latitud inicial
     * @param startLon longitud inicial
     * @param endLat   latitud final
     * @param endLon   longitud final
     * @return resultado de la validación con distancia y duración estimada
     */
    RouteValidationResponseDTO calculateTripDistance(
            BigDecimal startLat,
            BigDecimal startLon,
            BigDecimal endLat,
            BigDecimal endLon
    );

    /**
     * Valida la ruta usando Google Maps Directions API si hay API Key configurada.
     * Si no hay API Key, usa el cálculo por fórmula de Haversine como fallback.
     *
     * @param startLat latitud inicial
     * @param startLon longitud inicial
     * @param endLat   latitud final
     * @param endLon   longitud final
     * @return resultado con validación real o estimada de la ruta
     */
    RouteValidationResponseDTO validateRouteWithGoogleMaps(
            BigDecimal startLat,
            BigDecimal startLon,
            BigDecimal endLat,
            BigDecimal endLon
    );
}
