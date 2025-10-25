package co.edu.unbosque.notificationservice.service.strategy;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estrategia para envío de notificaciones por Email
 * Responsabilidad: SOLO enviar correos, NO persistir logs
 */
public class EmailNotificationStrategy implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationStrategy.class);
    private final JavaMailSender mailSender;

    public EmailNotificationStrategy(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String body) throws Exception {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El destinatario no puede estar vacío");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject != null ? subject : "Sin asunto");
            helper.setText(body != null ? body : "", true); // true = HTML

            mailSender.send(message);
            log.info("Email enviado exitosamente a: {}", to);

        } catch (MessagingException e) {
            log.error("Error al construir el mensaje MIME para: {}", to, e);
            throw new Exception("Error al construir el mensaje de correo", e);
        } catch (Exception e) {
            log.error("Error al enviar correo a: {}", to, e);
            throw new Exception("Error al enviar correo electrónico", e);
        }
    }

    @Override
    public String getChannelType() {
        return "EMAIL";
    }
}