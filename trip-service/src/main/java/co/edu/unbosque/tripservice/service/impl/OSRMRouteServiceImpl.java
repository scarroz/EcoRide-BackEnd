package co.edu.unbosque.tripservice.service.impl;

import co.edu.unbosque.tripservice.dto.OSMRouteResponseDTO;
import co.edu.unbosque.tripservice.dto.RouteSegmentDTO;
import co.edu.unbosque.tripservice.service.OSRMRouteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para integración con OSRM (Open Source Routing Machine)
 * Alternativa gratuita a Google Maps Directions API
 *
 * OSRM Demo Server: http://router.project-osrm.org
 * Documentación: http://project-osrm.org/docs/v5.24.0/api/
 */
@Service
public class OSRMRouteServiceImpl implements OSRMRouteService{

    @Value("${osrm.api.url:http://router.project-osrm.org}")
    private String osrmBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OSRMRouteServiceImpl(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * Obtiene ruta real entre dos puntos usando OSRM
     * Usa perfil "bike" para rutas ciclables
     */
    @Cacheable(value = "routes", key = "#startLon + '_' + #startLat + '_' + #endLon + '_' + #endLat")
    public OSMRouteResponseDTO getRoute(
            BigDecimal startLon,
            BigDecimal startLat,
            BigDecimal endLon,
            BigDecimal endLat
    ) {
        try {
            // OSRM formato: /route/v1/{profile}/{lon},{lat};{lon},{lat}
            String url = String.format(
                    "%s/route/v1/bike/%s,%s;%s,%s?overview=full&geometries=geojson&steps=true",
                    osrmBaseUrl,
                    startLon, startLat,
                    endLon, endLat
            );

            System.out.println("Consultando OSRM: " + url);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (!"Ok".equals(root.path("code").asText())) {
                throw new RuntimeException("OSRM error: " + root.path("message").asText());
            }

            JsonNode route = root.path("routes").get(0);

            // Extraer información de la ruta
            BigDecimal totalDistance = BigDecimal.valueOf(route.path("distance").asDouble())
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP); // m -> km

            Integer totalDuration = route.path("duration").asInt() / 60; // s -> min

            // Extraer geometría (coordenadas de la ruta)
            JsonNode geometry = route.path("geometry").path("coordinates");
            List<List<BigDecimal>> coordinates = new ArrayList<>();

            geometry.forEach(coord -> {
                List<BigDecimal> point = List.of(
                        BigDecimal.valueOf(coord.get(0).asDouble()).setScale(6, RoundingMode.HALF_UP), // lon
                        BigDecimal.valueOf(coord.get(1).asDouble()).setScale(6, RoundingMode.HALF_UP)  // lat
                );
                coordinates.add(point);
            });

            // Extraer segmentos de la ruta
            List<RouteSegmentDTO> segments = extractSegments(route.path("legs").get(0).path("steps"));

            return new OSMRouteResponseDTO(
                    "OK",
                    totalDistance,
                    totalDuration,
                    segments,
                    coordinates
            );

        } catch (Exception e) {
            System.err.println("Error consultando OSRM: " + e.getMessage());
            throw new RuntimeException("Error obteniendo ruta desde OSRM", e);
        }
    }

    /**
     * Extrae segmentos detallados de la ruta (calles, tipo de vía)
     */
    private List<RouteSegmentDTO> extractSegments(JsonNode steps) {
        List<RouteSegmentDTO> segments = new ArrayList<>();

        steps.forEach(step -> {
            JsonNode maneuver = step.path("maneuver");
            JsonNode location = maneuver.path("location");

            if (location.size() >= 2) {
                BigDecimal lon = BigDecimal.valueOf(location.get(0).asDouble());
                BigDecimal lat = BigDecimal.valueOf(location.get(1).asDouble());

                BigDecimal distance = BigDecimal.valueOf(step.path("distance").asDouble() / 1000.0)
                        .setScale(2, RoundingMode.HALF_UP);

                Integer duration = step.path("duration").asInt() / 60;

                String roadType = step.path("name").asText("Unknown Road");

                RouteSegmentDTO segment = new RouteSegmentDTO(
                        lat, lon, lat, lon, // Start y End se pueden calcular con siguiente step
                        distance,
                        duration,
                        roadType
                );

                segments.add(segment);
            }
        });

        return segments;
    }

    /**
     * Genera puntos intermedios a lo largo de la ruta para simulación realista
     * Devuelve N puntos equidistantes sobre la geometría
     */
    public List<List<BigDecimal>> interpolateRoutePoints(
            List<List<BigDecimal>> geometry,
            int numberOfPoints
    ) {
        if (geometry == null || geometry.size() < 2) {
            return geometry;
        }

        List<List<BigDecimal>> interpolated = new ArrayList<>();
        double step = (double) (geometry.size() - 1) / (numberOfPoints - 1);

        for (int i = 0; i < numberOfPoints; i++) {
            int index = (int) Math.round(i * step);
            index = Math.min(index, geometry.size() - 1);
            interpolated.add(geometry.get(index));
        }

        return interpolated;
    }

    /**
     * Valida si una coordenada está sobre la ruta (con tolerancia)
     */
    public boolean isPointOnRoute(
            List<List<BigDecimal>> routeGeometry,
            BigDecimal lat,
            BigDecimal lon,
            double toleranceMeters
    ) {
        for (List<BigDecimal> point : routeGeometry) {
            double distance = calculateHaversineDistance(
                    lat.doubleValue(), lon.doubleValue(),
                    point.get(1).doubleValue(), point.get(0).doubleValue()
            ) * 1000; // km -> m

            if (distance <= toleranceMeters) {
                return true;
            }
        }
        return false;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}