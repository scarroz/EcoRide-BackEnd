package co.edu.unbosque.tripservice.controller;

import co.edu.unbosque.tripservice.dto.*;
import co.edu.unbosque.tripservice.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trips")
@CrossOrigin(origins = "*")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /**
     * Iniciar viaje (automáticamente inicia simulación IoT)
     */
    @PostMapping("/start")
    public ResponseEntity<?> startTrip(@RequestBody TripStartRequestDTO request) {
        try {
            System.out.println("\n ===== API: INICIO DE VIAJE =====");
            System.out.println("Usuario: " + request.userId());
            System.out.println("Bicicleta: " + request.bicycleId());
            System.out.println("Estación: " + request.stationId());
            System.out.println("Tipo: " + request.tripType());
            System.out.println("===================================\n");

            TripResponseDTO response = tripService.startTrip(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Viaje iniciado exitosamente. Simulación IoT en progreso.");
            result.put("data", response);
            result.put("iot_status", "Telemetría GPS publicándose cada 5 segundos");
            result.put("kafka_topics", List.of(
                    "bicycle-telemetry",
                    "bicycle-battery",
                    "bicycle-alerts"
            ));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error iniciando viaje: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Finalizar viaje (automáticamente detiene simulación IoT)
     */
    @PostMapping("/end")
    public ResponseEntity<?> endTrip(@RequestBody TripEndRequestDTO request) {
        try {
            System.out.println("\n ===== API: FINALIZACIÓN DE VIAJE =====");
            System.out.println("Trip ID: " + request.tripId());
            System.out.println("Estación destino: " + request.endStationId());
            System.out.println("=========================================\n");

            TripResponseDTO response = tripService.endTrip(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Viaje finalizado exitosamente");
            result.put("data", response);
            result.put("iot_status", "Simulación IoT detenida");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error finalizando viaje: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Obtener viajes de un usuario
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserTrips(@PathVariable Long userId) {
        try {
            List<TripResponseDTO> trips = tripService.getUserTrips(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Viajes obtenidos: " + trips.size(),
                    "data", trips
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Obtener detalle de un viaje
     */
    @GetMapping("/{tripId}")
    public ResponseEntity<?> getTripDetail(@PathVariable Long tripId) {
        try {
            TripDetailDTO trip = tripService.getTripDetail(tripId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Detalle de viaje obtenido",
                    "data", trip
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", "Viaje no encontrado: " + e.getMessage()
                    ));
        }
    }

    /**
     * Obtener viajes activos (con simulación IoT)
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveTrips() {
        try {
            List<TripResponseDTO> trips = tripService.getActiveTrips();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", trips.size() + " viajes activos con simulación IoT",
                    "data", trips
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * 🧪 ENDPOINT DE PRUEBA: Iniciar viaje de prueba automático
     */
    @PostMapping("/test/start-demo")
    public ResponseEntity<?> startDemoTrip() {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("INICIANDO VIAJE DE PRUEBA AUTOMÁTICO");
            System.out.println("=".repeat(60));

            TripStartRequestDTO request = new TripStartRequestDTO(
                    1L,           // userId
                    1L,           // bicycleId
                    1L,           // stationId (debe ser Metro para LAST_MILE)
                    "LAST_MILE",  // tripType
                    "WALLET"      // paymentSource
            );

            TripResponseDTO response = tripService.startTrip(request);

            System.out.println("Viaje iniciado con ID: " + response.id());
            System.out.println("Telemetría IoT activa - GPS cada 5 segundos");
            System.out.println("Monitorea Kafka con:");
            System.out.println("   kafka-console-consumer --topic bicycle-telemetry");
            System.out.println("=".repeat(60) + "\n");

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Viaje de prueba iniciado exitosamente");
            result.put("data", response);
            result.put("instructions", Map.of(
                    "1", "La telemetría IoT se está publicando automáticamente",
                    "2", "Revisa los logs para ver GPS, velocidad, batería",
                    "3", "Para finalizar: POST /trips/end con tripId=" + response.id(),
                    "4", "Para monitorear: kafka-console-consumer --topic bicycle-telemetry"
            ));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error en viaje de prueba: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error iniciando viaje de prueba: " + e.getMessage(),
                            "details", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * 🧪 Endpoint para finalizar el viaje de prueba fácilmente
     */
    @PostMapping("/test/end-demo/{tripId}")
    public ResponseEntity<?> endDemoTrip(@PathVariable Long tripId) {
        try {
            TripEndRequestDTO request = new TripEndRequestDTO(
                    tripId,
                    2L,  // endStationId
                    null, // finalLatitude (opcional)
                    null  // finalLongitude (opcional)
            );

            TripResponseDTO response = tripService.endTrip(request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Viaje de prueba finalizado",
                    "data", response
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Health check específico del servicio de viajes
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "trip-service",
                "status", "UP",
                "iot_simulation", "ENABLED",
                "osrm_integration", "ACTIVE",
                "kafka_integration", "CONNECTED"
        ));
    }
}