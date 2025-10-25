package co.edu.unbosque.tripservice.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ActiveTripTelemetry {

    // ================================================
    // ATRIBUTOS
    // ================================================

    /**
     * ID del viaje (Trip.id)
     */
    private final Long tripId;

    /**
     * ID de la bicicleta (Bicycle.id)
     */
    private final Long bicycleId;

    /**
     * ID del usuario que está haciendo el viaje
     */
    private final Long userId;

    /**
     * Lista de coordenadas GPS de la ruta desde OSRM
     * Formato: [[lon1, lat1], [lon2, lat2], ...]
     * Ejemplo: [[-74.0856, 4.6482], [-74.0850, 4.6490], ...]
     */
    private final List<List<BigDecimal>> routePoints;

    /**
     * Timestamp de cuando inició el viaje
     */
    private final LocalDateTime startTime;

    /**
     * Nivel de batería actual (solo para eléctricas)
     * null para bicicletas mecánicas
     */
    private Integer batteryLevel;

    /**
     * Índice del punto GPS actual en la ruta
     * Va de 0 a routePoints.size()-1
     */
    private int currentPointIndex = 0;

    // ================================================
    // CONSTRUCTOR
    // ================================================

    /**
     * Constructor principal
     *
     * @param tripId       ID del viaje
     * @param bicycleId    ID de la bicicleta
     * @param userId       ID del usuario
     * @param routePoints  Lista de coordenadas [lon, lat] desde OSRM
     * @param startTime    Timestamp de inicio
     * @param batteryLevel Nivel de batería inicial (null si es mecánica)
     */
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

    // ================================================
    // MÉTODOS PRINCIPALES
    // ================================================

    /**
     * Avanza al siguiente punto GPS de la ruta
     * Llamado cada 5 segundos por el scheduler
     */
    public void advanceToNextPoint() {
        if (currentPointIndex < routePoints.size() - 1) {
            currentPointIndex++;
        }
    }

    /**
     * Obtiene el punto GPS actual
     *
     * @return Lista [longitude, latitude] o null si terminó la ruta
     */
    public List<BigDecimal> getCurrentPoint() {
        if (currentPointIndex >= routePoints.size()) {
            return null;
        }
        return routePoints.get(currentPointIndex);
    }

    /**
     * Verifica si la ruta se completó
     *
     * @return true si llegó al último punto
     */
    public boolean isCompleted() {
        return currentPointIndex >= routePoints.size() - 1;
    }

    /**
     * Calcula el porcentaje de progreso del viaje
     *
     * @return Porcentaje entre 0 y 100
     */
    public int getProgressPercentage() {
        if (routePoints.isEmpty()) return 0;
        return (int) ((currentPointIndex * 100.0) / (routePoints.size() - 1));
    }

    /**
     * Obtiene el próximo punto GPS (para calcular bearing)
     *
     * @return Lista [longitude, latitude] o null si es el último punto
     */
    public List<BigDecimal> getNextPoint() {
        if (currentPointIndex + 1 >= routePoints.size()) {
            return null;
        }
        return routePoints.get(currentPointIndex + 1);
    }

    /**
     * Disminuye la batería en un porcentaje
     *
     * @param drainAmount Cantidad a disminuir (ej: 0.8 = 0.8%)
     */
    public void drainBattery(double drainAmount) {
        if (batteryLevel != null) {
            batteryLevel = Math.max(0, (int) (batteryLevel - drainAmount));
        }
    }

    /**
     * Verifica si la batería está baja
     *
     * @return true si batería <= 20%
     */
    public boolean isLowBattery() {
        return batteryLevel != null && batteryLevel <= 20;
    }

    /**
     * Verifica si la batería está crítica
     *
     * @return true si batería <= 10%
     */
    public boolean isCriticalBattery() {
        return batteryLevel != null && batteryLevel <= 10;
    }

    /**
     * Calcula cuántos puntos quedan por recorrer
     *
     * @return Número de puntos restantes
     */
    public int getRemainingPoints() {
        return Math.max(0, routePoints.size() - currentPointIndex - 1);
    }

    /**
     * Obtiene el tiempo transcurrido desde el inicio
     *
     * @return Duración en minutos
     */
    public long getElapsedMinutes() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
    }

    // ================================================
    // GETTERS Y SETTERS
    // ================================================

    public Long getTripId() {
        return tripId;
    }

    public Long getBicycleId() {
        return bicycleId;
    }

    public Long getUserId() {
        return userId;
    }

    public List<List<BigDecimal>> getRoutePoints() {
        return routePoints;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public int getCurrentPointIndex() {
        return currentPointIndex;
    }

    public void setCurrentPointIndex(int currentPointIndex) {
        this.currentPointIndex = currentPointIndex;
    }
}
