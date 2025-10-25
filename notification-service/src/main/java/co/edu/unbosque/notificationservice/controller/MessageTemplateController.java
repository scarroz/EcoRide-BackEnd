package co.edu.unbosque.notificationservice.controller;

import co.edu.unbosque.notificationservice.dto.MessageTemplateDTO;
import co.edu.unbosque.notificationservice.service.MessageTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/templates")
public class MessageTemplateController {

    private final MessageTemplateService service;

    public MessageTemplateController(MessageTemplateService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MessageTemplateDTO> createTemplate(@RequestBody MessageTemplateDTO dto) {
        return ResponseEntity.ok(service.createTemplate(dto));
    }

    @GetMapping("/{name}")
    public ResponseEntity<MessageTemplateDTO> getByName(@PathVariable String name) {
        return ResponseEntity.ok(service.getTemplateByName(name));
    }

    @GetMapping
    public ResponseEntity<List<MessageTemplateDTO>> getAll() {
        return ResponseEntity.ok(service.getAllTemplates());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Integer id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
