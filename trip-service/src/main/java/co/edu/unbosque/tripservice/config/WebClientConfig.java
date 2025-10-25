package co.edu.unbosque.tripservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${payment.service.url:http://localhost:8082}")
    private String paymentServiceUrl;

    @Bean(name = "userWebClient")
    public WebClient userWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    @Bean(name = "paymentWebClient")
    public WebClient paymentWebClient() {
        return WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
    }
}