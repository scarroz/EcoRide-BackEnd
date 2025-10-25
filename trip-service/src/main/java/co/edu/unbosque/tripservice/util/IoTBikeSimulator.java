package co.edu.unbosque.tripservice.util;


import co.edu.unbosque.tripservice.dto.OSMRouteResponseDTO;
import co.edu.unbosque.tripservice.dto.event.BicycleTelemetryEvent;
import co.edu.unbosque.tripservice.dto.event.*;
import co.edu.unbosque.tripservice.model.Trip;
import co.edu.unbosque.tripservice.service.OSRMRouteService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

/**
 * Simulador Automático de Dispositivos IoT
 * Simula telemetría GPS realista para viajes activos
 */
@Component
@ConditionalOnProperty(name = "iot.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class IoTBikeSimulator {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OSRMRouteService osrmService;
    private final ObjectMapper objectMapper;

    @Value("${iot.simulation.telemetry.interval.seconds:5}")
    private int telemetryInterval;

    @Value("${iot.simulation.battery.drain.rate:0.8}")
    private double batteryDrainRate;

    @Value("${iot.simulation.speed.min:12.0}")
    private double speedMin;

    @Value("${iot.simulation.speed.max:25.0}")
    private double speedMax;

    // Mapa de simulaciones activas
    private final ConcurrentHashMap<Long, TripSimulation> activeSimulations = new ConcurrentHashMap<>();

    // Executor para simular en paralelo
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    public IoTBikeSimulator(
            KafkaTemplate<String, Object> kafkaTemplate,
            OSRMRouteService osrmService,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.osrmService = osrmService;
        this.objectMapper = objectMapper;
    }

    /**
     * Iniciar simulación de viaje
     */
    @Async
    public void startSimulation(Trip trip) {
        if (activeSimulations.containsKey(trip.getId())) {
            System.out.println("⚠️ Simulación ya existe para trip: " + trip.getId());
            return;
        }

        try {
            System.out.println("🚀 Iniciando simulación IoT para trip: " + trip.getId());

            // Obtener ruta real desde OSRM
            BigDecimal startLat = trip.getStartStation().getLatitude();
            BigDecimal startLon = trip.getStartStation().getLongitude();

            // Para simulación, usar una posición cercana como destino
            // En producción real, esto vendría de la estación destino
            BigDecimal endLat = startLat.add(BigDecimal.valueOf(0.015)); // ~1.5km
            BigDecimal endLon = startLon.add(BigDecimal.valueOf(0.020));

            OSMRouteResponseDTO route = osrmService.getRoute(startLon, startLat, endLon, endLat);

            // Interpolar puntos para simulación suave
            int numPoints = Math.max(route.totalDuration() * 60 / telemetryInterval, 20);
            List<List<BigDecimal>> interpolatedRoute = osrmService.interpolateRoutePoints(
                    route.geometry(),
                    numPoints
            );

            // Crear objeto de simulación
            TripSimulation simulation = new TripSimulation(
                    trip.getId(),
                    trip.getBicycle().getId(),
                    trip.getUserId(),
                    interpolatedRoute,
                    "ELECTRIC".equals(trip.getBicycle().getType()) ? 100 : null
            );

            activeSimulations.put(trip.getId(), simulation);

            // Programar tarea de telemetría
            ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                    () -> publishTelemetry(simulation),
                    0,
                    telemetryInterval,
                    TimeUnit.SECONDS
            );

            simulation.setScheduledFuture(future);

            System.out.println("Simulación iniciada - " + interpolatedRoute.size() + " puntos en ruta");

        } catch (Exception e) {
            System.err.println("Error iniciando simulación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Detener simulación de viaje
     */
    public void stopSimulation(Long tripId) {
        TripSimulation simulation = activeSimulations.remove(tripId);

        if (simulation != null) {
            if (simulation.getScheduledFuture() != null) {
                simulation.getScheduledFuture().cancel(false);
            }
            System.out.println("Simulación detenida para trip: " + tripId);
        }
    }

    /**
     * Publicar telemetría del viaje
     */
    private void publishTelemetry(TripSimulation simulation) {
        try {
            // Avanzar al siguiente punto
            simulation.advanceToNextPoint();

            List<BigDecimal> currentPoint = simulation.getCurrentPoint();

            if (currentPoint == null || simulation.isCompleted()) {
                System.out.println("Ruta completada para trip: " + simulation.getTripId());
                stopSimulation(simulation.getTripId());
                return;
            }

            BigDecimal lat = currentPoint.get(1);
            BigDecimal lon = currentPoint.get(0);

            // Calcular velocidad aleatoria dentro del rango
            BigDecimal speed = BigDecimal.valueOf(
                    speedMin + Math.random() * (speedMax - speedMin)
            ).setScale(1, RoundingMode.HALF_UP);

            // Calcular dirección
            Integer bearing = calculateBearing(simulation);

            // Calcular distancia acumulada
            BigDecimal distanceFromStart = calculateDistanceFromStart(simulation);

            // Disminuir batería si es eléctrica
            if (simulation.getBatteryLevel() != null) {
                int newBattery = Math.max(0,
                        (int) (simulation.getBatteryLevel() - batteryDrainRate));
                simulation.setBatteryLevel(newBattery);

                // Publicar batería cada 10 iteraciones
                if (simulation.getCurrentIndex() % 10 == 0) {
                    publishBatteryStatus(simulation);
                }

                // Publicar alerta si batería baja
                if (newBattery <= 20 && simulation.getCurrentIndex() % 5 == 0) {
                    publishLowBatteryAlert(simulation, lat, lon);
                }
            }

            // Crear evento de telemetría
            BicycleTelemetryEvent event = new BicycleTelemetryEvent(
                    simulation.getBicycleId(),
                    simulation.getTripId(),
                    LocalDateTime.now().toString(),
                    lat,
                    lon,
                    speed,
                    bearing,
                    BigDecimal.valueOf(2600 + Math.random() * 100), // Altitud Bogotá
                    BigDecimal.valueOf(3 + Math.random() * 5), // Precisión GPS
                    simulation.getBatteryLevel(),
                    "UNLOCKED",
                    distanceFromStart
            );

            // Publicar a Kafka
            kafkaTemplate.send("bicycle-telemetry", simulation.getBicycleId().toString(), event);

            System.out.println("📡 Telemetría [Trip " + simulation.getTripId() + "] - " +
                    "Pos: " + lat + "," + lon + " | " +
                    "Speed: " + speed + " km/h | " +
                    "Distance: " + distanceFromStart + " km");

        } catch (Exception e) {
            System.err.println("Error publicando telemetría: " + e.getMessage());
        }
    }

    /**
     * Publicar estado de batería
     */
    private void publishBatteryStatus(TripSimulation simulation) {
        try {
            String alertLevel = simulation.getBatteryLevel() > 50 ? "OK" :
                    simulation.getBatteryLevel() > 20 ? "LOW" : "CRITICAL";

            BatteryStatusEvent event = new BatteryStatusEvent(
                    simulation.getBicycleId(),
                    simulation.getBatteryLevel(),
                    false,
                    BigDecimal.valueOf(simulation.getBatteryLevel() * 0.5),
                    LocalDateTime.now().toString(),
                    alertLevel
            );

            kafkaTemplate.send("bicycle-battery", simulation.getBicycleId().toString(), event);
            System.out.println("🔋 Batería [Bike " + simulation.getBicycleId() + "]: " +
                    simulation.getBatteryLevel() + "% [" + alertLevel + "]");

        } catch (Exception e) {
            System.err.println("❌ Error publicando batería: " + e.getMessage());
        }
    }

    /**
     * Publicar alerta de batería baja
     */
    private void publishLowBatteryAlert(TripSimulation simulation, BigDecimal lat, BigDecimal lon) {
        try {
            BicycleAlertEvent alert = new BicycleAlertEvent(
                    simulation.getBicycleId(),
                    simulation.getTripId(),
                    "LOW_BATTERY",
                    simulation.getBatteryLevel() <= 10 ? "CRITICAL" : "WARNING",
                    "Batería baja: " + simulation.getBatteryLevel() + "%",
                    LocalDateTime.now().toString(),
                    new LocationData(lat, lon),
                    java.util.Map.of(
                            "battery_level", simulation.getBatteryLevel(),
                            "estimated_range", simulation.getBatteryLevel() * 0.5
                    )
            );

            kafkaTemplate.send("bicycle-alerts", simulation.getBicycleId().toString(), alert);
            System.out.println("⚠️ ALERTA batería baja [Bike " + simulation.getBicycleId() + "]: " +
                    simulation.getBatteryLevel() + "%");

        } catch (Exception e) {
            System.err.println("❌ Error publicando alerta: " + e.getMessage());
        }
    }

    /**
     * Calcular distancia acumulada desde el inicio
     */
    private BigDecimal calculateDistanceFromStart(TripSimulation simulation) {
        if (simulation.getCurrentIndex() == 0) {
            return BigDecimal.ZERO;
        }

        double totalDistance = 0;
        List<List<BigDecimal>> route = simulation.getRoutePoints();

        for (int i = 0; i < simulation.getCurrentIndex(); i++) {
            if (i + 1 < route.size()) {
                List<BigDecimal> p1 = route.get(i);
                List<BigDecimal> p2 = route.get(i + 1);

                totalDistance += calculateHaversineDistance(
                        p1.get(1).doubleValue(), p1.get(0).doubleValue(),
                        p2.get(1).doubleValue(), p2.get(0).doubleValue()
                );
            }
        }

        return BigDecimal.valueOf(totalDistance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcular dirección entre puntos
     */
    private Integer calculateBearing(TripSimulation simulation) {
        List<List<BigDecimal>> route = simulation.getRoutePoints();
        int currentIndex = simulation.getCurrentIndex();

        if (currentIndex >= route.size() - 1) {
            return 0;
        }

        List<BigDecimal> current = route.get(currentIndex);
        List<BigDecimal> next = route.get(currentIndex + 1);

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

    /**
     * Calcular distancia Haversine en km
     */
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

    /**
     * Clase interna para gestionar simulación de viaje
     */
    private static class TripSimulation {
        private final Long tripId;
        private final Long bicycleId;
        private final Long userId;
        private final List<List<BigDecimal>> routePoints;
        private Integer batteryLevel;
        private int currentIndex = 0;
        private ScheduledFuture<?> scheduledFuture;

        public TripSimulation(Long tripId, Long bicycleId, Long userId,
                              List<List<BigDecimal>> routePoints, Integer batteryLevel) {
            this.tripId = tripId;
            this.bicycleId = bicycleId;
            this.userId = userId;
            this.routePoints = routePoints;
            this.batteryLevel = batteryLevel;
        }

        public void advanceToNextPoint() {
            if (currentIndex < routePoints.size() - 1) {
                currentIndex++;
            }
        }

        public List<BigDecimal> getCurrentPoint() {
            if (currentIndex >= routePoints.size()) {
                return null;
            }
            return routePoints.get(currentIndex);
        }

        public boolean isCompleted() {
            return currentIndex >= routePoints.size() - 1;
        }

        // Getters y Setters
        public Long getTripId() { return tripId; }
        public Long getBicycleId() { return bicycleId; }
        public Long getUserId() { return userId; }
        public List<List<BigDecimal>> getRoutePoints() { return routePoints; }
        public Integer getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
        public int getCurrentIndex() { return currentIndex; }
        public ScheduledFuture<?> getScheduledFuture() { return scheduledFuture; }
        public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }
    }
}
