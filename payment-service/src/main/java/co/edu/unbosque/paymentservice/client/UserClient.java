package co.edu.unbosque.paymentservice.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class UserClient {

    @Autowired
    private WebClient userWebClient;

    /**
     * Envía la notificación al UserService para actualizar el saldo de la wallet
     * después de una recarga exitosa en Stripe.
     */
    public void updateWalletBalance(Long userId, BigDecimal amount) {
        userWebClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/wallet/recharge")
                        .queryParam("userId", userId)
                        .queryParam("amount", amount)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(error -> {
                    System.err.println("Error notificando al UserService: " + error.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}
