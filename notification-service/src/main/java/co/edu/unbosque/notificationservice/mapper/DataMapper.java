package co.edu.unbosque.notificationservice.mapper;

import co.edu.unbosque.notificationservice.dto.*;
import co.edu.unbosque.notificationservice.model.*;
import org.springframework.stereotype.Component;

@Component
public class DataMapper {

    public MessageTemplateDTO toDTO(MessageTemplate e) {
        if (e == null) return null;
        return new MessageTemplateDTO(e.getId(), e.getCode(), e.getSubject(), e.getBody());
    }

    public MessageTemplate toEntity(MessageTemplateDTO dto) {
        if (dto == null) return null;
        MessageTemplate entity = new MessageTemplate();
        entity.setId(dto.id());
        entity.setCode(dto.code());
        entity.setSubject(dto.subject());
        entity.setBody(dto.body());
        return entity;
    }

    public NotificationResponseDTO toNotificationDTO(NotificationLog e) {
        if (e == null) return null;
        String code = e.getTemplate() != null ? e.getTemplate().getCode() : null;
        return new NotificationResponseDTO(e.getId(), e.getUserId(), code, e.getStatus(), e.getSentAt());
    }
}
