package co.edu.unbosque.notificationservice.service.factory;

import co.edu.unbosque.notificationservice.service.strategy.EmailNotificationStrategy;
import co.edu.unbosque.notificationservice.service.strategy.NotificationChannel;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Factory para crear instancias de canales de notificación
 * Patrón: Factory Method
 */
@Component
public class NotificationFactory {

    private final JavaMailSender mailSender;

    public NotificationFactory(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Crea un canal de notificación según el tipo especificado
     * @param channelType Tipo de canal (EMAIL, SMS, PUSH, etc.)
     * @return Instancia del canal correspondiente
     * @throws IllegalArgumentException Si el canal no está soportado
     */
    public NotificationChannel createChannel(String channelType) {
        if (channelType == null || channelType.isBlank()) {
            throw new IllegalArgumentException("El tipo de canal no puede ser nulo o vacío");
        }

        return switch (channelType.toUpperCase()) {
            case "EMAIL" -> new EmailNotificationStrategy(mailSender);
            // Aquí puedes agregar más canales en el futuro:
            // case "SMS" -> new SmsNotificationStrategy(smsProvider);
            // case "PUSH" -> new PushNotificationStrategy(pushService);
            default -> throw new IllegalArgumentException("Canal no soportado: " + channelType);
        };
    }
}