package co.edu.unbosque.tripservice.service;



import co.edu.unbosque.tripservice.dto.OSMRouteResponseDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio para la integración con OSRM (Open Source Routing Machine),
 * usado para obtener rutas reales, validar posiciones y generar puntos intermedios.
 */
public interface OSRMRouteService {

    /**
     * Obtiene la ruta real entre dos puntos geográficos usando OSRM.
     * Utiliza el perfil "bike" para rutas ciclables.
     *
     * @param startLon Longitud inicial
     * @param startLat Latitud inicial
     * @param endLon   Longitud final
     * @param endLat   Latitud final
     * @return respuesta estructurada con distancia, duración, segmentos y geometría
     */
    OSMRouteResponseDTO getRoute(
            BigDecimal startLon,
            BigDecimal startLat,
            BigDecimal endLon,
            BigDecimal endLat
    );

    /**
     * Genera puntos intermedios a lo largo de la ruta,
     * devolviendo N puntos equidistantes sobre la geometría.
     * Esto permite simulaciones IoT más realistas.
     *
     * @param geometry        lista de coordenadas [lon, lat] de la ruta
     * @param numberOfPoints  número total de puntos deseados
     * @return lista de coordenadas interpoladas
     */
    List<List<BigDecimal>> interpolateRoutePoints(
            List<List<BigDecimal>> geometry,
            int numberOfPoints
    );

    /**
     * Verifica si una coordenada específica se encuentra sobre la ruta,
     * dentro de una tolerancia en metros.
     *
     * @param routeGeometry   geometría de la ruta
     * @param lat             latitud a verificar
     * @param lon             longitud a verificar
     * @param toleranceMeters tolerancia máxima en metros
     * @return true si el punto está dentro de la ruta, false en caso contrario
     */
    boolean isPointOnRoute(
            List<List<BigDecimal>> routeGeometry,
            BigDecimal lat,
            BigDecimal lon,
            double toleranceMeters
    );
}

