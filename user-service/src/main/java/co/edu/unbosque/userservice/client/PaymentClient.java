package co.edu.unbosque.userservice.client;

import co.edu.unbosque.userservice.dto.PaymentCardRequestDTO;
import co.edu.unbosque.userservice.dto.WalletRechargeRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class PaymentClient {

    @Autowired
    private WebClient paymentWebClient;

    public void registerCard(PaymentCardRequestDTO request) {
        try {
            System.out.println("Llamando a Payment-Service para registrar tarjeta...");
            System.out.println("   userId: " + request.userId());
            System.out.println("   email: " + request.email());
            System.out.println("   paymentMethodId: " + request.paymentMethodId());

            paymentWebClient.post()
                    .uri("/api/payments/cards/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), PaymentCardRequestDTO.class)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            System.out.println("Tarjeta registrada exitosamente en Payment-Service");
        } catch (WebClientResponseException e) {
            System.err.println("Error en Payment-Service: " + e.getResponseBodyAsString());
            throw new RuntimeException("Error registrando tarjeta: " + e.getResponseBodyAsString(), e);
        }
    }

    public void rechargeWallet(WalletRechargeRequestDTO request) {
        try {
            System.out.println("========================================");
            System.out.println("ENVIANDO REQUEST DESDE USER-SERVICE:");
            System.out.println("   userId: " + request.userId());
            System.out.println("   amount: " + request.amount());
            System.out.println("   paymentMethodId: " + request.paymentMethodId());
            System.out.println("   paymentMethodId es null? " + (request.paymentMethodId() == null));
            System.out.println("   Clase del DTO: " + request.getClass().getName());
            System.out.println("========================================");

            String response = paymentWebClient.post()
                    .uri("/api/payments/wallet/recharge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), WalletRechargeRequestDTO.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Respuesta del Payment-Service: " + response);
        } catch (WebClientResponseException e) {
            System.err.println("Error HTTP: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            throw new RuntimeException("Error recargando wallet: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            System.err.println("Error general: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error recargando wallet: " + e.getMessage(), e);
        }
    }
}