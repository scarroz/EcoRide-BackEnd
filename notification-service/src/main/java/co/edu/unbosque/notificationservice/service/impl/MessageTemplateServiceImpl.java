package co.edu.unbosque.notificationservice.service.impl;

import co.edu.unbosque.notificationservice.dto.MessageTemplateDTO;
import co.edu.unbosque.notificationservice.mapper.DataMapper;
import co.edu.unbosque.notificationservice.model.MessageTemplate;
import co.edu.unbosque.notificationservice.repository.MessageTemplateRepository;
import co.edu.unbosque.notificationservice.service.MessageTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private final MessageTemplateRepository repository;
    private final DataMapper mapper;

    public MessageTemplateServiceImpl(MessageTemplateRepository repository, DataMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }


    @Override
    public MessageTemplateDTO createTemplate(MessageTemplateDTO dto) {
        MessageTemplate entity = mapper.toEntity(dto);
        return mapper.toDTO(repository.save(entity));
    }

    @Override
    public MessageTemplateDTO getTemplateByName(String name) {
        MessageTemplate template = repository.findByCode(name)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada: " + name));
        return mapper.toDTO(template);
    }

    @Override
    public List<MessageTemplateDTO> getAllTemplates() {
        return repository.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public void deleteTemplate(Integer id) {
        repository.deleteById(id);
    }
}
