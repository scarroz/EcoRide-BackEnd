package co.edu.unbosque.userservice.client;

import co.edu.unbosque.userservice.dto.NotificationRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class NotificationClient {

    private final WebClient webClient;


    public NotificationClient(@Value("${notification.service.url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void sendNotification(NotificationRequestDTO request) {
        try {
            webClient.post()
                    .uri("/api/notifications/send")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .onErrorResume(WebClientResponseException.class, e -> {
                        System.err.println("Error al enviar notificación: " + e.getMessage());
                        return Mono.empty();
                    })
                    .block(); // Bloqueamos porque estamos en contexto sincrónico
        } catch (Exception e) {
            System.err.println("Fallo general al enviar notificación: " + e.getMessage());
        }
    }


    public void sendPasswordRecoveryCode(Integer userId, String code) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", userId,
                    "code", code
            );

            webClient.post()
                    .uri("/api/notifications/password-recovery")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            System.out.println("Solicitud enviada al notification-service para userId=" + userId);

        } catch (WebClientResponseException e) {
            System.out.println("Error HTTP al enviar código de recuperación: "
                    + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.out.println("Error general al enviar código de recuperación: " + e.getMessage());
            throw new RuntimeException("Fallo al comunicarse con notification-service", e);
        }
    }


}
