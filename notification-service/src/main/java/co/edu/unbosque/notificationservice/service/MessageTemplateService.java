package co.edu.unbosque.notificationservice.service;

import co.edu.unbosque.notificationservice.dto.MessageTemplateDTO;
import java.util.List;

public interface MessageTemplateService {
    MessageTemplateDTO createTemplate(MessageTemplateDTO dto);
    MessageTemplateDTO getTemplateByName(String name);
    List<MessageTemplateDTO> getAllTemplates();
    void deleteTemplate(Integer id);
}
