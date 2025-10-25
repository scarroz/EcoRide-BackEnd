package co.edu.unbosque.notificationservice.dto;

public record MessageTemplateDTO(
        Integer id,
        String code,
        String subject,
        String body
) {}
