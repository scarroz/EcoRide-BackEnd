package co.edu.unbosque.tripservice.client;

import co.edu.unbosque.tripservice.dto.TripPaymentRequestDTO;
import co.edu.unbosque.tripservice.dto.TripPaymentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class PaymentServiceClient {

    @Autowired
    private WebClient paymentWebClient;

    public TripPaymentResponseDTO processTripPayment(TripPaymentRequestDTO request) {
        try {
            System.out.println("Procesando pago de viaje para usuario: " + request.userId());

            return paymentWebClient.post()
                    .uri("/api/payments/trip/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), TripPaymentRequestDTO.class)
                    .retrieve()
                    .bodyToMono(TripPaymentResponseDTO.class)
                    .block();

        } catch (WebClientResponseException e) {
            System.err.println("Error procesando pago: " + e.getResponseBodyAsString());
            throw new RuntimeException("Error al procesar pago del viaje");
        }
    }

    public void refundTripPayment(Long tripId, java.math.BigDecimal amount) {
        try {
            paymentWebClient.post()
                    .uri("/api/payments/trip/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(Map.of("tripId", tripId, "amount", amount)), Object.class)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            System.out.println("Reembolso procesado para trip: " + tripId);
        } catch (Exception e) {
            System.err.println("Error procesando reembolso: " + e.getMessage());
        }
    }
}
