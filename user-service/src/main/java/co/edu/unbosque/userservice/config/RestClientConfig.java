package co.edu.unbosque.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RestClientConfig {

    @Bean(name = "paymentWebClient")
    public WebClient paymentWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8082") // URL del Payment Service
                .build();
    }
}
