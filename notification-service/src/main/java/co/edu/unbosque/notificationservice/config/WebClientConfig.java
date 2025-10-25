package co.edu.unbosque.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${notification.user-service.base-url}")
    private String userServiceBaseUrl;

    @Bean
    public WebClient userWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceBaseUrl)
                .build();
    }
}
