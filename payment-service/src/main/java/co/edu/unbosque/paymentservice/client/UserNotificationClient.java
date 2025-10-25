package co.edu.unbosque.paymentservice.client;

import co.edu.unbosque.paymentservice.dto.WalletNotificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserNotificationClient {

    @Autowired
    private WebClient userWebClient;

    /**
     * Envía notificación al UserService para actualizar el saldo de la wallet.
     */
    public void notifyWalletRecharge(WalletNotificationDTO notification) {
        userWebClient.post()
                .uri("/wallet/notify-recharge")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(notification), WalletNotificationDTO.class)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(error -> {
                    System.err.println("Error notificando al UserService: " + error.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}
