package co.edu.unbosque.tripservice.client;


import co.edu.unbosque.tripservice.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class UserServiceClient {

    @Autowired
    private WebClient userWebClient;

    public UserValidationResponseDTO validateUser(Long userId) {
        try {
            System.out.println("Validando usuario: " + userId);

            return userWebClient.get()
                    .uri("/api/users/" + userId)
                    .retrieve()
                    .bodyToMono(UserValidationResponseDTO.class)
                    .block();

        } catch (WebClientResponseException e) {
            System.err.println("Error validando usuario: " + e.getResponseBodyAsString());
            throw new RuntimeException("Usuario no encontrado o inactivo");
        }
    }

    public void notifyTripStarted(Long userId, Long tripId) {
        try {
            userWebClient.post()
                    .uri("/api/notifications/trip-started")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(Map.of("userId", userId, "tripId", tripId)), Object.class)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            System.out.println("Notificacion de inicio de viaje enviada");
        } catch (Exception e) {
            System.err.println("Error enviando notificacion: " + e.getMessage());
        }
    }

    public void notifyTripCompleted(Long userId, Long tripId, java.math.BigDecimal cost) {
        try {
            userWebClient.post()
                    .uri("/api/notifications/trip-completed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(Map.of(
                            "userId", userId,
                            "tripId", tripId,
                            "cost", cost
                    )), Object.class)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            System.out.println("Notificacion de finalizacion de viaje enviada");
        } catch (Exception e) {
            System.err.println("Error enviando notificacion: " + e.getMessage());
        }
    }
}
