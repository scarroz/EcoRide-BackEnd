package co.edu.unbosque.tripservice.config;


import co.edu.unbosque.tripservice.model.Bicycle;
import co.edu.unbosque.tripservice.model.Station;
import co.edu.unbosque.tripservice.repository.BicycleRepository;
import co.edu.unbosque.tripservice.repository.StationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeData(
            StationRepository stationRepo,
            BicycleRepository bicycleRepo
    ) {
        return args -> {
            if (stationRepo.count() > 0) {
                System.out.println("Datos ya inicializados, omitiendo carga inicial");
                return;
            }

            System.out.println("Iniciando carga de datos del sistema...");

            // 1. ESTACIONES RESIDENCIALES (50 estaciones)
            List<Station> residentialStations = createResidentialStations();
            stationRepo.saveAll(residentialStations);
            System.out.println("50 estaciones residenciales creadas");

            // 2. ESTACIONES METRO (70 estaciones)
            List<Station> metroStations = createMetroStations();
            stationRepo.saveAll(metroStations);
            System.out.println("70 estaciones Metro creadas");

            // 3. ESTACIONES CENTRO FINANCIERO (50 estaciones)
            List<Station> financialStations = createFinancialStations();
            stationRepo.saveAll(financialStations);
            System.out.println("50 estaciones Centro Financiero creadas");

            // 4. BICICLETAS (2550 total: 1225 mecanicas, 1225 electricas)
            List<Station> allStations = stationRepo.findAll();
            List<Bicycle> bicycles = createBicycles(allStations);
            bicycleRepo.saveAll(bicycles);
            System.out.println(bicycles.size() + " bicicletas creadas (50% mecanicas, 50% electricas)");

            System.out.println("Inicializacion completada exitosamente");
            System.out.println("Total estaciones: " + stationRepo.count());
            System.out.println("Total bicicletas: " + bicycleRepo.count());
        };
    }

    private List<Station> createResidentialStations() {
        List<Station> stations = new ArrayList<>();

        // Coordenadas base para zonas residenciales en Bogota
        // Usaquén, Chapinero, Suba
        double[][] zones = {
                {4.70, -74.03}, // Usaquen
                {4.65, -74.05}, // Chapinero
                {4.75, -74.08}, // Suba
                {4.60, -74.10}, // Engativa
                {4.68, -74.12}  // Fontibon
        };

        for (int i = 0; i < 50; i++) {
            Station station = new Station();
            station.setName("Residencial " + (i + 1));
            station.setCapacity(15);
            station.setActive(true);

            // Distribuir en diferentes zonas
            int zone = i % zones.length;
            double latVariation = (Math.random() - 0.5) * 0.04;
            double lonVariation = (Math.random() - 0.5) * 0.04;

            station.setLatitude(new BigDecimal(zones[zone][0] + latVariation).setScale(6, BigDecimal.ROUND_HALF_UP));
            station.setLongitude(new BigDecimal(zones[zone][1] + lonVariation).setScale(6, BigDecimal.ROUND_HALF_UP));

            stations.add(station);
        }

        return stations;
    }

    private List<Station> createMetroStations() {
        List<Station> stations = new ArrayList<>();

        // Estaciones reales del Metro de Bogota
        String[] metroNames = {
                "Metro Calle 26", "Metro Heroes", "Metro Calle 63", "Metro Calle 72",
                "Metro Flores", "Metro Granja", "Metro Calle 127", "Metro Toberín",
                "Metro Alcalá", "Metro Suba Centro", "Metro Portal Américas",
                "Metro Banderas", "Metro Sevillana", "Metro Pradera", "Metro Mundo Aventura"
        };

        // Coordenadas base de estaciones Metro importantes
        double[][] metroCoords = {
                {4.6482, -74.0856}, // Calle 26
                {4.6538, -74.0627}, // Heroes
                {4.6654, -74.0548}, // Calle 63
                {4.6752, -74.0583}, // Calle 72
                {4.6890, -74.0450}  // Calle 100
        };

        for (int i = 0; i < 70; i++) {
            Station station = new Station();

            if (i < metroNames.length) {
                station.setName("Metro " + metroNames[i]);
            } else {
                station.setName("Metro Estación " + (i + 1));
            }

            station.setCapacity(15);
            station.setActive(true);

            if (i < metroCoords.length) {
                station.setLatitude(new BigDecimal(metroCoords[i][0]).setScale(6, BigDecimal.ROUND_HALF_UP));
                station.setLongitude(new BigDecimal(metroCoords[i][1]).setScale(6, BigDecimal.ROUND_HALF_UP));
            } else {
                int base = i % metroCoords.length;
                double latVariation = (Math.random() - 0.5) * 0.03;
                double lonVariation = (Math.random() - 0.5) * 0.03;

                station.setLatitude(new BigDecimal(metroCoords[base][0] + latVariation).setScale(6, BigDecimal.ROUND_HALF_UP));
                station.setLongitude(new BigDecimal(metroCoords[base][1] + lonVariation).setScale(6, BigDecimal.ROUND_HALF_UP));
            }

            stations.add(station);
        }

        return stations;
    }

    private List<Station> createFinancialStations() {
        List<Station> stations = new ArrayList<>();

        // Centro Internacional, La Candelaria, Zona T
        double[][] financialZones = {
                {4.6533, -74.0661}, // Centro Internacional
                {4.5981, -74.0758}, // La Candelaria
                {4.6677, -74.0488}, // Zona T / Zona Rosa
                {4.6097, -74.0817}, // San Diego
                {4.6450, -74.0900}  // Centro Historico
        };

        for (int i = 0; i < 50; i++) {
            Station station = new Station();
            station.setName("Centro Financiero " + (i + 1));
            station.setCapacity(15);
            station.setActive(true);

            int zone = i % financialZones.length;
            double latVariation = (Math.random() - 0.5) * 0.02;
            double lonVariation = (Math.random() - 0.5) * 0.02;

            station.setLatitude(new BigDecimal(financialZones[zone][0] + latVariation).setScale(6, BigDecimal.ROUND_HALF_UP));
            station.setLongitude(new BigDecimal(financialZones[zone][1] + lonVariation).setScale(6, BigDecimal.ROUND_HALF_UP));

            stations.add(station);
        }

        return stations;
    }

    private List<Bicycle> createBicycles(List<Station> stations) {
        List<Bicycle> bicycles = new ArrayList<>();
        int bicycleCount = 0;
        int stationIndex = 0;

        // Distribuir 15 bicicletas por estacion (170 estaciones × 15 = 2550)
        for (Station station : stations) {
            for (int i = 0; i < 15; i++) {
                Bicycle bicycle = new Bicycle();
                bicycle.setCode(String.format("BK-%04d", ++bicycleCount));

                // Alternar entre mecanica y electrica
                if (bicycleCount % 2 == 0) {
                    bicycle.setType("MECHANICAL");
                    bicycle.setBatteryLevel(null);
                } else {
                    bicycle.setType("ELECTRIC");
                    bicycle.setBatteryLevel(100); // Bateria completa inicialmente
                }

                bicycle.setStatus("AVAILABLE");
                bicycle.setLastStation(station);

                bicycles.add(bicycle);
            }
            stationIndex++;
        }

        return bicycles;
    }
}
