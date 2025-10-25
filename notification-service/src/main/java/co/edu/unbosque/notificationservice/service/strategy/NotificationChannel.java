package co.edu.unbosque.notificationservice.service.strategy;

/**
 * Interface para estrategias de envío de notificaciones
 * Responsabilidad ÚNICA: enviar mensajes por diferentes canales
 */
public interface NotificationChannel {
    /**
     * Envía un mensaje por el canal específico
     * @param to Destinatario
     * @param subject Asunto del mensaje
     * @param body Cuerpo del mensaje
     * @throws Exception Si ocurre un error en el envío
     */
    void send(String to, String subject, String body) throws Exception;

    /**
     * Retorna el tipo de canal (EMAIL, SMS, PUSH, etc.)
     */
    String getChannelType();
}