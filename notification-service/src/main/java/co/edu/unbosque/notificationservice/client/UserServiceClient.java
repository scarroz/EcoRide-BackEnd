package co.edu.unbosque.notificationservice.client;

import co.edu.unbosque.notificationservice.dto.UserEmailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class UserServiceClient {

    @Autowired
    private WebClient userWebClient;

    public UserEmailDTO getUserEmailById(Integer userId) {
        try {
            return userWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/users/{id}/email").build(userId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(UserEmailDTO.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Error consultando user-service: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Error comunicando con user-service: " + e.getMessage());
        }
    }
}
