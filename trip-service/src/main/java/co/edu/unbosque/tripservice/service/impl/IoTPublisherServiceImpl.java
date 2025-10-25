package co.edu.unbosque.tripservice.service.impl;

import co.edu.unbosque.tripservice.dto.OSMRouteResponseDTO;
import co.edu.unbosque.tripservice.dto.event.*;
import co.edu.unbosque.tripservice.dto.event.*;
import co.edu.unbosque.tripservice.model.Bicycle;
import co.edu.unbosque.tripservice.model.Trip;
import co.edu.unbosque.tripservice.repository.TripRepository;
import co.edu.unbosque.tripservice.service.IoTPublisherService;
import co.edu.unbosque.tripservice.service.OSRMRouteService;
import co.edu.unbosque.tripservice.util.ActiveTripTelemetry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IoTPublisherServiceImpl implements IoTPublisherService {


    private static final String TOPIC_TELEMETRY = "bicycle-telemetry";
    private static final String TOPIC_LOCK_STATUS = "bicycle-lock-status";
    private static final String TOPIC_BATTERY = "bicycle-battery";
    private static final String TOPIC_ALERTS = "bicycle-alerts";
    private static final String TOPIC_TRIP_EVENTS = "trip-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OSRMRouteService osrmService;
    private final TripRepository tripRepository;

    // Almacena telemetría activa con ruta real
    private final Map<Long, ActiveTripTelemetry> activeTrips = new ConcurrentHashMap<>();

    public IoTPublisherServiceImpl(
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            OSRMRouteService osrmService,
            TripRepository tripRepository
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.osrmService = osrmService;
        this.tripRepository = tripRepository;
    }

    // ================================================
    // INICIAR VIAJE - Obtiene ruta real de OSRM
    // ================================================

    public void startTripTelemetry(Trip trip) {
        try {
            System.out.println("Iniciando telemetria con ruta real para trip: " + trip.getId());

            BigDecimal startLat = trip.getStartStation().getLatitude();
            BigDecimal startLon = trip.getStartStation().getLongitude();

            // Para simular, usamos una estación cercana aleatoria como destino
            // En producción, se usaría la estación destino real una vez conocida
            BigDecimal endLat = startLat.add(BigDecimal.valueOf(0.02)); // ~2km
            BigDecimal endLon = startLon.add(BigDecimal.valueOf(0.02));

            // Obtener ruta real desde OSRM
            OSMRouteResponseDTO route = osrmService.getRoute(startLon, startLat, endLon, endLat);

            // Interpolar puntos para simulación suave (1 punto cada 5 segundos)
            int totalPoints = (int) (route.totalDuration() * 60 / 5); // 1 punto cada 5 seg
            List<List<BigDecimal>> interpolatedPoints = osrmService.interpolateRoutePoints(
                    route.geometry(),
                    Math.max(totalPoints, 20)
            );

            ActiveTripTelemetry telemetry = new ActiveTripTelemetry(
                    trip.getId(),
                    trip.getBicycle().getId(),
                    trip.getUserId(),
                    interpolatedPoints,
                    LocalDateTime.now(),
                    "ELECTRIC".equals(trip.getBicycle().getType()) ? 100 : null
            );

            activeTrips.put(trip.getId(), telemetry);

            // Publicar evento de inicio
            publishTripStarted(trip);

            System.out.println("Telemetría iniciada - Ruta: " + interpolatedPoints.size() + " puntos");

        } catch (Exception e) {
            System.err.println("Error iniciando telemetría: " + e.getMessage());
        }
    }

    // ================================================
    // DETENER VIAJE
    // ================================================

    public void stopTripTelemetry(Long tripId, BigDecimal distanceKm, BigDecimal cost) {
        ActiveTripTelemetry telemetry = activeTrips.remove(tripId);

        if (telemetry != null) {
            Trip trip = tripRepository.findById(tripId).orElse(null);
            if (trip != null) {
                publishTripCompleted(trip, distanceKm, cost);
            }
            System.out.println("Telemetría detenida para trip: " + tripId);
        }
    }

    // ================================================
    // TAREA PROGRAMADA - Publicar telemetría cada 5 seg
    // ================================================

    @Scheduled(fixedRate = 5000) // 5 segundos
    @Async
    public void publishActiveTripsTelemetry() {
        if (activeTrips.isEmpty()) {
            return;
        }

        activeTrips.values().forEach(telemetry -> {
            try {
                // Avanzar al siguiente punto de la ruta
                telemetry.advanceToNextPoint();

                List<BigDecimal> currentPoint = telemetry.getCurrentPoint();

                if (currentPoint == null) {
                    System.out.println("Viaje completado (ruta finalizada): " + telemetry.tripId);
                    return;
                }

                // Calcular velocidad (15-25 km/h típico en bicicleta)
                BigDecimal speed = BigDecimal.valueOf(15 + Math.random() * 10)
                        .setScale(1, RoundingMode.HALF_UP);

                // Calcular distancia acumulada
                BigDecimal distanceFromStart = calculateDistanceFromStart(telemetry);

                // Crear evento de telemetría
                BicycleTelemetryEvent event = new BicycleTelemetryEvent(
                        telemetry.bicycleId,
                        telemetry.tripId,
                        LocalDateTime.now().toString(),
                        currentPoint.get(1), // latitude
                        currentPoint.get(0), // longitude
                        speed,
                        calculateBearing(telemetry), // dirección
                        BigDecimal.valueOf(2600 + Math.random() * 100), // altitud Bogotá
                        BigDecimal.valueOf(5 + Math.random() * 5), // precisión GPS
                        telemetry.batteryLevel,
                        "UNLOCKED",
                        distanceFromStart
                );

                // Publicar a Kafka
                kafkaTemplate.send(TOPIC_TELEMETRY, telemetry.bicycleId.toString(), event);

                // Disminuir batería si es eléctrica
                if (telemetry.batteryLevel != null && telemetry.batteryLevel > 0) {
                    telemetry.batteryLevel = Math.max(0, telemetry.batteryLevel - 1);

                    if (telemetry.batteryLevel % 10 == 0) {
                        publishBatteryStatus(telemetry.bicycleId, telemetry.batteryLevel);
                    }

                    if (telemetry.batteryLevel <= 20 && telemetry.batteryLevel % 5 == 0) {
                        publishLowBatteryAlert(telemetry);
                    }
                }

                System.out.println("Telemetría publicada - Bike: " + telemetry.bicycleId +
                        " Pos: " + currentPoint.get(1) + "," + currentPoint.get(0) +
                        " Speed: " + speed + " km/h");

            } catch (Exception e) {
                System.err.println("Error publicando telemetría: " + e.getMessage());
            }
        });
    }

    // ================================================
    // PUBLICAR CAMBIO DE ESTADO DE CANDADO
    // ================================================

    public void publishLockStatus(Bicycle bicycle, String status, Long userId, Long stationId) {
        try {
            LockStatusEvent event = new LockStatusEvent(
                    bicycle.getId(),
                    status,
                    stationId,
                    userId,
                    LocalDateTime.now().toString(),
                    bicycle.getLastStation() != null ? new LocationData(
                            bicycle.getLastStation().getLatitude(),
                            bicycle.getLastStation().getLongitude()
                    ) : null
            );

            kafkaTemplate.send(TOPIC_LOCK_STATUS, bicycle.getId().toString(), event);
            System.out.println("Lock status publicado - Bike: " + bicycle.getId() + " -> " + status);

        } catch (Exception e) {
            System.err.println("Error publicando lock status: " + e.getMessage());
        }
    }

    // ================================================
    // PUBLICAR ESTADO DE BATERÍA
    // ================================================

    public void publishBatteryStatus(Long bicycleId, Integer batteryLevel) {
        try {
            String alertLevel = batteryLevel > 50 ? "OK" :
                    batteryLevel > 20 ? "LOW" : "CRITICAL";

            BigDecimal estimatedRange = BigDecimal.valueOf(batteryLevel * 0.5)
                    .setScale(1, RoundingMode.HALF_UP);

            BatteryStatusEvent event = new BatteryStatusEvent(
                    bicycleId,
                    batteryLevel,
                    false,
                    estimatedRange,
                    LocalDateTime.now().toString(),
                    alertLevel
            );

            kafkaTemplate.send(TOPIC_BATTERY, bicycleId.toString(), event);
            System.out.println("Batería publicada - Bike: " + bicycleId + " -> " + batteryLevel + "%");

        } catch (Exception e) {
            System.err.println("Error publicando batería: " + e.getMessage());
        }
    }

    // ================================================
    // PUBLICAR ALERTAS
    // ================================================

    private void publishLowBatteryAlert(ActiveTripTelemetry telemetry) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("battery_level", telemetry.batteryLevel);
            metadata.put("estimated_range", telemetry.batteryLevel * 0.5);

            BicycleAlertEvent alert = new BicycleAlertEvent(
                    telemetry.bicycleId,
                    telemetry.tripId,
                    "LOW_BATTERY",
                    telemetry.batteryLevel <= 10 ? "CRITICAL" : "WARNING",
                    "Batería baja: " + telemetry.batteryLevel + "%",
                    LocalDateTime.now().toString(),
                    new LocationData(
                            telemetry.getCurrentPoint().get(1),
                            telemetry.getCurrentPoint().get(0)
                    ),
                    metadata
            );

            kafkaTemplate.send(TOPIC_ALERTS, telemetry.bicycleId.toString(), alert);
            System.out.println("Alerta batería baja publicada - Bike: " + telemetry.bicycleId);

        } catch (Exception e) {
            System.err.println("Error publicando alerta: " + e.getMessage());
        }
    }

    // ================================================
    // EVENTOS DE VIAJE (Event Sourcing)
    // ================================================

    private void publishTripStarted(Trip trip) {
        try {
            TripStartedEvent event = new TripStartedEvent(
                    trip.getId(),
                    trip.getUserId(),
                    trip.getBicycle().getId(),
                    trip.getStartStation().getId(),
                    trip.getTripType(),
                    LocalDateTime.now().toString(),
                    new LocationData(
                            trip.getStartStation().getLatitude(),
                            trip.getStartStation().getLongitude()
                    )
            );

            kafkaTemplate.send(TOPIC_TRIP_EVENTS, trip.getId().toString(), event);
            System.out.println("Evento TripStarted publicado - Trip: " + trip.getId());

        } catch (Exception e) {
            System.err.println("Error publicando TripStarted: " + e.getMessage());
        }
    }

    private void publishTripCompleted(Trip trip, BigDecimal distanceKm, BigDecimal cost) {
        try {
            long duration = java.time.Duration.between(trip.getStartTime(), trip.getEndTime()).toMinutes();

            TripCompletedEvent event = new TripCompletedEvent(
                    trip.getId(),
                    trip.getUserId(),
                    trip.getBicycle().getId(),
                    trip.getEndStation() != null ? trip.getEndStation().getId() : null,
                    distanceKm,
                    duration,
                    cost,
                    LocalDateTime.now().toString(),
                    trip.getEndStation() != null ? new LocationData(
                            trip.getEndStation().getLatitude(),
                            trip.getEndStation().getLongitude()
                    ) : null
            );

            kafkaTemplate.send(TOPIC_TRIP_EVENTS, trip.getId().toString(), event);
            System.out.println("Evento TripCompleted publicado - Trip: " + trip.getId());

        } catch (Exception e) {
            System.err.println("Error publicando TripCompleted: " + e.getMessage());
        }
    }

    // ================================================
    // MÉTODOS AUXILIARES
    // ================================================

    private BigDecimal calculateDistanceFromStart(ActiveTripTelemetry telemetry) {
        if (telemetry.currentPointIndex == 0) {
            return BigDecimal.ZERO;
        }

        double totalDistance = 0;
        for (int i = 0; i < telemetry.currentPointIndex; i++) {
            List<BigDecimal> p1 = telemetry.routePoints.get(i);
            List<BigDecimal> p2 = telemetry.routePoints.get(i + 1);

            totalDistance += calculateHaversineDistance(
                    p1.get(1).doubleValue(), p1.get(0).doubleValue(),
                    p2.get(1).doubleValue(), p2.get(0).doubleValue()
            );
        }

        return BigDecimal.valueOf(totalDistance).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer calculateBearing(ActiveTripTelemetry telemetry) {
        if (telemetry.currentPointIndex >= telemetry.routePoints.size() - 1) {
            return 0;
        }

        List<BigDecimal> current = telemetry.getCurrentPoint();
        List<BigDecimal> next = telemetry.routePoints.get(telemetry.currentPointIndex + 1);

        double lat1 = Math.toRadians(current.get(1).doubleValue());
        double lat2 = Math.toRadians(next.get(1).doubleValue());
        double lon1 = Math.toRadians(current.get(0).doubleValue());
        double lon2 = Math.toRadians(next.get(0).doubleValue());

        double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (int) ((bearing + 360) % 360);
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

    // ================================================
    // CLASE INTERNA - Telemetría Activa
    // ================================================

    private static class ActiveTripTelemetry {
        Long tripId;
        Long bicycleId;
        Long userId;
        List<List<BigDecimal>> routePoints;
        LocalDateTime startTime;
        Integer batteryLevel;
        int currentPointIndex = 0;

        public ActiveTripTelemetry(
                Long tripId,
                Long bicycleId,
                Long userId,
                List<List<BigDecimal>> routePoints,
                LocalDateTime startTime,
                Integer batteryLevel
        ) {
            this.tripId = tripId;
            this.bicycleId = bicycleId;
            this.userId = userId;
            this.routePoints = routePoints;
            this.startTime = startTime;
            this.batteryLevel = batteryLevel;
        }

        public void advanceToNextPoint() {
            if (currentPointIndex < routePoints.size() - 1) {
                currentPointIndex++;
            }
        }

        public List<BigDecimal> getCurrentPoint() {
            if (currentPointIndex >= routePoints.size()) {
                return null;
            }
            return routePoints.get(currentPointIndex);
        }

        public boolean isRouteCompleted() {
            return currentPointIndex >= routePoints.size() - 1;
        }
    }
}