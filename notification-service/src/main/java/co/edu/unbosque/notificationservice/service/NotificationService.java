package co.edu.unbosque.notificationservice.service;

import co.edu.unbosque.notificationservice.dto.NotificationRequestDTO;
import co.edu.unbosque.notificationservice.dto.NotificationResponseDTO;

public interface NotificationService {
    NotificationResponseDTO sendNotification(NotificationRequestDTO request);
     NotificationResponseDTO sendPasswordRecoveryCode(Integer userId, String code);

    }
