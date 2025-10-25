package co.edu.unbosque.tripservice.service.impl;

import co.edu.unbosque.tripservice.dto.RouteValidationResponseDTO;
import co.edu.unbosque.tripservice.service.RouteValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RouteValidationServiceImpl implements RouteValidationService {

    @Value("${google.maps.api.key:#{null}}")
    private String googleMapsApiKey;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double STATION_PROXIMITY_THRESHOLD_METERS = 30.0;

    private final RestTemplate restTemplate;

    public RouteValidationServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Valida que la posicion final este dentro del radio de la estacion
     */
    public RouteValidationResponseDTO validateFinalPosition(
            BigDecimal stationLat,
            BigDecimal stationLon,
            BigDecimal finalLat,
            BigDecimal finalLon
    ) {
        System.out.println("Validando posicion final GPS");
        System.out.println("Estacion: " + stationLat + ", " + stationLon);
        System.out.println("Posicion final: " + finalLat + ", " + finalLon);

        double distanceMeters = calculateHaversineDistance(
                stationLat.doubleValue(),
                stationLon.doubleValue(),
                finalLat.doubleValue(),
                finalLon.doubleValue()
        ) * 1000; // Convertir a metros

        System.out.println("Distancia a estacion: " + distanceMeters + " metros");

        boolean isValid = distanceMeters <= STATION_PROXIMITY_THRESHOLD_METERS;

        if (!isValid) {
            return new RouteValidationResponseDTO(
                    false,
                    null,
                    null,
                    "La bicicleta debe anclarse dentro de los " + STATION_PROXIMITY_THRESHOLD_METERS + " metros de la estacion"
            );
        }

        return new RouteValidationResponseDTO(
                true,
                BigDecimal.valueOf(distanceMeters / 1000).setScale(2, RoundingMode.HALF_UP),
                0L,
                "Posicion validada correctamente"
        );
    }

    /**
     * Calcula distancia entre inicio y fin del viaje
     */
    public RouteValidationResponseDTO calculateTripDistance(
            BigDecimal startLat,
            BigDecimal startLon,
            BigDecimal endLat,
            BigDecimal endLon
    ) {
        double distanceKm = calculateHaversineDistance(
                startLat.doubleValue(),
                startLon.doubleValue(),
                endLat.doubleValue(),
                endLon.doubleValue()
        );

        // Validar que la ruta este dentro de Bogota (4.5 - 4.8 lat, -74.2 - -73.9 lon)
        boolean withinBogota = isWithinBogota(startLat, startLon) && isWithinBogota(endLat, endLon);

        if (!withinBogota) {
            return new RouteValidationResponseDTO(
                    false,
                    null,
                    null,
                    "La ruta debe estar dentro de los limites de Bogota"
            );
        }

        return new RouteValidationResponseDTO(
                true,
                BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP),
                estimateDurationMinutes(distanceKm),
                "Ruta validada"
        );
    }

    /**
     * Formula de Haversine para calcular distancia entre dos puntos GPS
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Valida que las coordenadas esten dentro de Bogota
     */
    private boolean isWithinBogota(BigDecimal lat, BigDecimal lon) {
        double latitude = lat.doubleValue();
        double longitude = lon.doubleValue();

        // Limites aproximados de Bogota
        boolean latInRange = latitude >= 4.45 && latitude <= 4.85;
        boolean lonInRange = longitude >= -74.25 && longitude <= -73.95;

        return latInRange && lonInRange;
    }

    /**
     * Estima duracion del viaje basado en distancia
     * Asume velocidad promedio de 15 km/h en bicicleta
     */
    private Long estimateDurationMinutes(double distanceKm) {
        double averageSpeedKmh = 15.0;
        double durationHours = distanceKm / averageSpeedKmh;
        return Math.round(durationHours * 60);
    }

    /**
     * Integracion con Google Maps Directions API (opcional si hay API key)
     */
    public RouteValidationResponseDTO validateRouteWithGoogleMaps(
            BigDecimal startLat,
            BigDecimal startLon,
            BigDecimal endLat,
            BigDecimal endLon
    ) {
        if (googleMapsApiKey == null || googleMapsApiKey.isEmpty()) {
            System.out.println("Google Maps API key no configurada, usando calculo Haversine");
            return calculateTripDistance(startLat, startLon, endLat, endLon);
        }

        try {
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/directions/json?origin=%s,%s&destination=%s,%s&mode=bicycling&key=%s",
                    startLat, startLon, endLat, endLon, googleMapsApiKey
            );

            // Realizar llamada a Google Maps API
            // Procesar respuesta y extraer distancia y duracion real
            // Por ahora usamos fallback a Haversine

            return calculateTripDistance(startLat, startLon, endLat, endLon);

        } catch (Exception e) {
            System.err.println("Error consultando Google Maps: " + e.getMessage());
            return calculateTripDistance(startLat, startLon, endLat, endLon);
        }
    }
}