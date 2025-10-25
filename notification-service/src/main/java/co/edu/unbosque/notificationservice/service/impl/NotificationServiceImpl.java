package co.edu.unbosque.notificationservice.service.impl;

import co.edu.unbosque.notificationservice.client.UserServiceClient;
import co.edu.unbosque.notificationservice.dto.*;
import co.edu.unbosque.notificationservice.mapper.DataMapper;
import co.edu.unbosque.notificationservice.model.MessageTemplate;
import co.edu.unbosque.notificationservice.model.NotificationLog;
import co.edu.unbosque.notificationservice.repository.MessageTemplateRepository;
import co.edu.unbosque.notificationservice.repository.NotificationLogRepository;
import co.edu.unbosque.notificationservice.service.NotificationService;
import co.edu.unbosque.notificationservice.service.factory.NotificationFactory;
import co.edu.unbosque.notificationservice.service.strategy.NotificationChannel;
import co.edu.unbosque.notificationservice.util.TemplateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación del servicio de notificaciones
 * Responsabilidad: Orquestar el proceso de envío y gestionar la persistencia
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final String DEFAULT_CHANNEL = "EMAIL";

    private final MessageTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final UserServiceClient userClient;
    private final NotificationFactory notificationFactory;
    private final DataMapper mapper;

    public NotificationServiceImpl(
            MessageTemplateRepository templateRepository,
            NotificationLogRepository logRepository,
            UserServiceClient userClient,
            NotificationFactory notificationFactory,
            DataMapper mapper
    ) {
        this.templateRepository = templateRepository;
        this.logRepository = logRepository;
        this.userClient = userClient;
        this.notificationFactory = notificationFactory;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public NotificationResponseDTO sendNotification(NotificationRequestDTO request) {
        log.info("Iniciando envío de notificación para usuario: {} con template: {}",
                request.userId(), request.templateCode());

        // 1. Recuperar plantilla desde BD (entidad MANAGED)
        MessageTemplate template = templateRepository.findByCode(request.templateCode())
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada: " + request.templateCode()));

        // 2. Obtener email del usuario desde el servicio externo
        UserEmailDTO userEmail = userClient.getUserEmailById(request.userId());
        if (userEmail == null || userEmail.email() == null || userEmail.email().isBlank()) {
            throw new RuntimeException("No se encontró email válido para el usuario: " + request.userId());
        }

        // 3. Procesar plantilla con variables dinámicas
        Map<String, String> variables = buildTemplateVariables(userEmail);
        String processedBody = TemplateProcessor.processTemplate(template.getBody(), variables);

        // 4. Crear log ANTES de intentar enviar (entidad MANAGED)
        NotificationLog notificationLog = createPendingLog(request.userId(), template);

        // 5. Determinar canal de envío
        String channel = (request.channel() != null && !request.channel().isBlank())
                ? request.channel()
                : DEFAULT_CHANNEL;

        // 6. Intentar enviar la notificación
        try {
            NotificationChannel notificationChannel = notificationFactory.createChannel(channel);
            notificationChannel.send(userEmail.email(), template.getSubject(), processedBody);

            // Envío exitoso
            notificationLog.setStatus("SENT");
            log.info("Notificación enviada exitosamente a usuario: {}", request.userId());

        } catch (Exception e) {
            // Fallo en el envío
            notificationLog.setStatus("FAILED");
            log.error("Error al enviar notificación a usuario: {}", request.userId(), e);
            // No lanzamos excepción, registramos el fallo
        }

        // 7. Actualizar timestamp y persistir
        notificationLog.setSentAt(LocalDateTime.now());
        notificationLog = logRepository.save(notificationLog);

        // 8. Retornar DTO con la información del envío
        return new NotificationResponseDTO(
                notificationLog.getId(),
                notificationLog.getUserId(),
                template.getCode(),
                notificationLog.getStatus(),
                notificationLog.getSentAt()
        );
    }

    @Override
    @Transactional
    public NotificationResponseDTO sendPasswordRecoveryCode(Integer userId, String code) {
        System.out.println("Enviando código de recuperación '" + code + "' para usuario: " + userId);

        // Obtener template MANAGED desde la BD
        MessageTemplate template = templateRepository.findByCode("PASSWORD_RECOVERY")
                .orElseThrow(() -> new RuntimeException("Plantilla PASSWORD_RECOVERY no encontrada"));

        UserEmailDTO userEmail = userClient.getUserEmailById(userId);
        if (userEmail == null || userEmail.email() == null || userEmail.email().isBlank()) {
            throw new RuntimeException("No se encontró email válido para el usuario: " + userId);
        }

        // Construir variables con el CÓDIGO
        Map<String, String> variables = buildTemplateVariables(userEmail);
        variables.put("code", code); // ← CÓDIGO para el email

        String processedBody = TemplateProcessor.processTemplate(template.getBody(), variables);

        // Crear y guardar el log en una sola operación
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setUserId(userId);
        notificationLog.setTemplate(template); // Template ya es MANAGED
        notificationLog.setStatus("PENDING");
        notificationLog.setSentAt(LocalDateTime.now());

        try {
            NotificationChannel channel = notificationFactory.createChannel(DEFAULT_CHANNEL);
            channel.send(userEmail.email(), template.getSubject(), processedBody);
            notificationLog.setStatus("SENT");
            System.out.println("Código de recuperación enviado exitosamente a " + userEmail.email());
        } catch (Exception e) {
            notificationLog.setStatus("FAILED");
            System.err.println("Error al enviar código: " + e.getMessage());
            throw new RuntimeException("Error al enviar código de recuperación", e);
        }

        // Guardar solo UNA VEZ al final
        notificationLog = logRepository.save(notificationLog);

        return mapper.toNotificationDTO(notificationLog);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Crea un log de notificación en estado PENDING
     */
    private NotificationLog createPendingLog(Integer userId, MessageTemplate template) {
        NotificationLog log = new NotificationLog();
        log.setUserId(userId);
        log.setTemplate(template); // Template ya es MANAGED por JPA
        log.setStatus("PENDING");
        log.setSentAt(LocalDateTime.now());
        return logRepository.save(log); // Persiste inmediatamente
    }

    /**
     * Construye el mapa de variables para procesar la plantilla
     */
    private Map<String, String> buildTemplateVariables(UserEmailDTO userEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("username", userEmail.fullName() != null ? userEmail.fullName() : "Usuario");
        variables.put("date", LocalDate.now().toString());
        return variables;
    }

    /**
     * Genera un código de recuperación de 6 dígitos
     */
    private String generateRecoveryCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}