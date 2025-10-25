package co.edu.unbosque.userservice.service.util;

import co.edu.unbosque.userservice.client.NotificationClient;
import co.edu.unbosque.userservice.dto.NotificationRequestDTO;
import co.edu.unbosque.userservice.model.UserAccount;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegistrationListener {

    private final NotificationClient notificationClient;

    public UserRegistrationListener(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @TransactionalEventListener
    public void handleUserRegistered(UserAccount user) {
        try {
            NotificationRequestDTO notification = new NotificationRequestDTO(
                    user.getId(),
                    "WELCOME",
                    "EMAIL",
                    null
            );
            notificationClient.sendNotification(notification);
            System.out.println("Correo de bienvenida enviado a: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Error al enviar correo de bienvenida: " + e.getMessage());
        }
    }
}

