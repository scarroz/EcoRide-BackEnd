package co.edu.unbosque.notificationservice;

import co.edu.unbosque.notificationservice.client.UserServiceClient;
import co.edu.unbosque.notificationservice.dto.NotificationRequestDTO;
import co.edu.unbosque.notificationservice.dto.NotificationResponseDTO;
import co.edu.unbosque.notificationservice.dto.UserEmailDTO;
import co.edu.unbosque.notificationservice.mapper.DataMapper;
import co.edu.unbosque.notificationservice.model.MessageTemplate;
import co.edu.unbosque.notificationservice.model.NotificationLog;
import co.edu.unbosque.notificationservice.repository.MessageTemplateRepository;
import co.edu.unbosque.notificationservice.repository.NotificationLogRepository;
import co.edu.unbosque.notificationservice.service.factory.NotificationFactory;
import co.edu.unbosque.notificationservice.service.impl.NotificationServiceImpl;
import co.edu.unbosque.notificationservice.service.strategy.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl - Unit Tests")
public class NotificationServiceImplTest {

    @Mock
    private MessageTemplateRepository templateRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private UserServiceClient userClient;

    @Mock
    private NotificationFactory notificationFactory;

    @Mock
    private DataMapper mapper;

    @Mock
    private NotificationChannel notificationChannel;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private MessageTemplate mockTemplate;
    private UserEmailDTO mockUserEmail;
    private NotificationLog mockNotificationLog;

    @BeforeEach
    void setUp() {
        // Configurar template mock
        mockTemplate = new MessageTemplate();
        mockTemplate.setId(1);
        mockTemplate.setCode("PASSWORD_RECOVERY");
        mockTemplate.setSubject("Código de Recuperación");
        mockTemplate.setBody("<html><body><h1>Hola {{username}}</h1><p>Tu código: {{code}}</p><p>Fecha: {{date}}</p></body></html>");

        // Configurar user email mock
        mockUserEmail = new UserEmailDTO(1, "Juan Pérez", "juan@example.com");

        // Configurar notification log mock
        mockNotificationLog = new NotificationLog();
        mockNotificationLog.setId(1);
        mockNotificationLog.setUserId(1);
        mockNotificationLog.setTemplate(mockTemplate);
        mockNotificationLog.setStatus("SENT");
        mockNotificationLog.setSentAt(LocalDateTime.now());
    }

    // ==================== TESTS PARA sendNotification ====================

    @Test
    @DisplayName("Debería enviar notificación genérica exitosamente")
    void deberiaEnviarNotificacionGenericaExitosamente() throws Exception {
        // Given
        NotificationRequestDTO request = new NotificationRequestDTO(1, "WELCOME", null);
        MessageTemplate welcomeTemplate = new MessageTemplate();
        welcomeTemplate.setId(2);
        welcomeTemplate.setCode("WELCOME");
        welcomeTemplate.setSubject("Bienvenido");
        welcomeTemplate.setBody("<html><body><h1>Bienvenido {{username}}</h1></body></html>");

        when(templateRepository.findByCode("WELCOME")).thenReturn(Optional.of(welcomeTemplate));
        when(userClient.getUserEmailById(1)).thenReturn(mockUserEmail);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        when(logRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> {
            NotificationLog log = invocation.getArgument(0);
            log.setId(1);
            return log;
        });
        doNothing().when(notificationChannel).send(anyString(), anyString(), anyString());

        // When
        NotificationResponseDTO response = notificationService.sendNotification(request);

        // Then
        assertNotNull(response);
        assertEquals(1, response.id());
        assertEquals(1, response.userId());
        assertEquals("WELCOME", response.templateCode());
        assertEquals("SENT", response.status());

        verify(templateRepository, times(1)).findByCode("WELCOME");
        verify(userClient, times(1)).getUserEmailById(1);
        verify(notificationChannel, times(1)).send(eq("juan@example.com"), eq("Bienvenido"), anyString());
        verify(logRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando la plantilla no existe")
    void deberiaLanzarExcepcionCuandoPlantillaNoExiste() {
        // Given
        NotificationRequestDTO request = new NotificationRequestDTO(1, "INVALID_TEMPLATE", null);
        when(templateRepository.findByCode("INVALID_TEMPLATE")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.sendNotification(request));

        assertEquals("Plantilla no encontrada: INVALID_TEMPLATE", exception.getMessage());
        verify(templateRepository, times(1)).findByCode("INVALID_TEMPLATE");
        verify(userClient, never()).getUserEmailById(anyInt());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el email del usuario es nulo")
    void deberiaLanzarExcepcionCuandoEmailEsNulo() {
        // Given
        NotificationRequestDTO request = new NotificationRequestDTO(1, "WELCOME", null);
        UserEmailDTO emailNulo = new UserEmailDTO(1, "Juan", null);

        when(templateRepository.findByCode("WELCOME")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(1)).thenReturn(emailNulo);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.sendNotification(request));

        assertEquals("No se encontró email válido para el usuario: 1", exception.getMessage());
        verify(templateRepository, times(1)).findByCode("WELCOME");
        verify(userClient, times(1)).getUserEmailById(1);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el email está vacío")
    void deberiaLanzarExcepcionCuandoEmailEstaVacio() {
        // Given
        NotificationRequestDTO request = new NotificationRequestDTO(1, "WELCOME", null);
        UserEmailDTO emailVacio = new UserEmailDTO(1, "Juan", "");

        when(templateRepository.findByCode("WELCOME")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(1)).thenReturn(emailVacio);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.sendNotification(request));

        assertEquals("No se encontró email válido para el usuario: 1", exception.getMessage());
    }

    @Test
    @DisplayName("Debería marcar como FAILED cuando falla el envío de notificación")
    void deberiaMarcarcComoFailedCuandoFallaEnvioNotificacion() throws Exception {
        // Given
        NotificationRequestDTO request = new NotificationRequestDTO(1, "WELCOME", null);

        when(templateRepository.findByCode("WELCOME")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(1)).thenReturn(mockUserEmail);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        when(logRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> {
            NotificationLog log = invocation.getArgument(0);
            log.setId(1);
            return log;
        });
        doThrow(new RuntimeException("SMTP Error")).when(notificationChannel).send(anyString(), anyString(), anyString());

        // When
        NotificationResponseDTO response = notificationService.sendNotification(request);

        // Then
        assertNotNull(response);
        assertEquals("FAILED", response.status());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(logCaptor.capture());

        NotificationLog finalLog = logCaptor.getAllValues().get(1);
        assertEquals("FAILED", finalLog.getStatus());
    }

    // ==================== TESTS PARA sendPasswordRecoveryCode ====================

    @Test
    @DisplayName("Debería enviar código de recuperación exitosamente")
    void deberiaEnviarCodigoRecuperacionExitosamente() throws Exception {
        // Given
        Integer userId = 1;
        String code = "123456";

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(userId)).thenReturn(mockUserEmail);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        when(mapper.toNotificationDTO(any(NotificationLog.class))).thenReturn(
                new NotificationResponseDTO(1, 1, "PASSWORD_RECOVERY", "SENT", LocalDateTime.now())
        );
        doNothing().when(notificationChannel).send(anyString(), anyString(), anyString());

        // When
        NotificationResponseDTO response = notificationService.sendPasswordRecoveryCode(userId, code);

        // Then
        assertNotNull(response);
        assertEquals(1, response.userId());
        assertEquals("PASSWORD_RECOVERY", response.templateCode());
        assertEquals("SENT", response.status());

        verify(templateRepository, times(1)).findByCode("PASSWORD_RECOVERY");
        verify(userClient, times(1)).getUserEmailById(userId);
        verify(notificationFactory, times(1)).createChannel("EMAIL");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationChannel, times(1)).send(
                eq("juan@example.com"),
                eq("Código de Recuperación"),
                bodyCaptor.capture()
        );

        String sentBody = bodyCaptor.getValue();
        assertTrue(sentBody.contains(code), "El body debe contener el código " + code);
        assertTrue(sentBody.contains("Juan Pérez"), "El body debe contener el nombre del usuario");
        assertFalse(sentBody.contains("{{code}}"), "No debe contener el placeholder {{code}}");
        assertFalse(sentBody.contains("{{username}}"), "No debe contener el placeholder {{username}}");

        verify(logRepository, times(1)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando template PASSWORD_RECOVERY no existe")
    void deberiaLanzarExcepcionCuandoTemplatePasswordRecoveryNoExiste() {
        // Given
        Integer userId = 1;
        String code = "123456";

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.sendPasswordRecoveryCode(userId, code));

        assertEquals("Plantilla PASSWORD_RECOVERY no encontrada", exception.getMessage());
        verify(templateRepository, times(1)).findByCode("PASSWORD_RECOVERY");
        verify(userClient, never()).getUserEmailById(anyInt());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el usuario no tiene email válido")
    void deberiaLanzarExcepcionCuandoUsuarioSinEmailValido() {
        // Given
        Integer userId = 1;
        String code = "123456";
        UserEmailDTO emailVacio = new UserEmailDTO(1, "Juan", "");

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(userId)).thenReturn(emailVacio);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.sendPasswordRecoveryCode(userId, code));

        assertEquals("No se encontró email válido para el usuario: 1", exception.getMessage());
        verify(userClient, times(1)).getUserEmailById(userId);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando falla el envío del código de recuperación")
    void deberiaLanzarExcepcionCuandoFallaEnvioCodigoRecuperacion() throws Exception {
        // Given
        Integer userId = 1;
        String code = "123456";

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(userId)).thenReturn(mockUserEmail);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        doThrow(new RuntimeException("SMTP Connection Failed"))
                .when(notificationChannel).send(anyString(), anyString(), anyString());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.sendPasswordRecoveryCode(userId, code));

        assertEquals("Error al enviar código de recuperación", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("SMTP Connection Failed", exception.getCause().getMessage());

        verify(notificationChannel, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debería procesar correctamente las variables del template incluyendo código")
    void deberiaProcesarCorrectamenteVariablesDelTemplateIncluyendoCodigo() throws Exception {
        // Given
        Integer userId = 1;
        String code = "987654";

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(userId)).thenReturn(mockUserEmail);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        when(mapper.toNotificationDTO(any(NotificationLog.class))).thenReturn(
                new NotificationResponseDTO(1, 1, "PASSWORD_RECOVERY", "SENT", LocalDateTime.now())
        );
        doNothing().when(notificationChannel).send(anyString(), anyString(), anyString());

        // When
        notificationService.sendPasswordRecoveryCode(userId, code);

        // Then
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationChannel).send(anyString(), anyString(), bodyCaptor.capture());

        String processedBody = bodyCaptor.getValue();

        // Verificar que NO contiene placeholders
        assertFalse(processedBody.contains("{{username}}"), "No debe contener placeholder {{username}}");
        assertFalse(processedBody.contains("{{code}}"), "No debe contener placeholder {{code}}");
        assertFalse(processedBody.contains("{{date}}"), "No debe contener placeholder {{date}}");

        // Verificar que contiene los valores procesados
        assertTrue(processedBody.contains("Juan Pérez"), "Debe contener el nombre procesado");
        assertTrue(processedBody.contains("987654"), "Debe contener el código procesado");
    }

    @Test
    @DisplayName("Debería guardar log con status SENT cuando el envío del código es exitoso")
    void deberiaGuardarLogConStatusSentCuandoEnvioCodigoExitoso() throws Exception {
        // Given
        Integer userId = 1;
        String code = "555555";

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(userId)).thenReturn(mockUserEmail);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        when(mapper.toNotificationDTO(any(NotificationLog.class))).thenReturn(
                new NotificationResponseDTO(1, 1, "PASSWORD_RECOVERY", "SENT", LocalDateTime.now())
        );
        doNothing().when(notificationChannel).send(anyString(), anyString(), anyString());

        // When
        notificationService.sendPasswordRecoveryCode(userId, code);

        // Then
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getValue();
        assertEquals("SENT", savedLog.getStatus());
        assertEquals(userId, savedLog.getUserId());
        assertEquals(mockTemplate, savedLog.getTemplate());
        assertNotNull(savedLog.getSentAt());
    }

    @Test
    @DisplayName("Debería manejar correctamente usuario sin nombre usando valor por defecto")
    void deberiaManejarCorrectamenteUsuarioSinNombreUsandoValorPorDefecto() throws Exception {
        // Given
        Integer userId = 1;
        String code = "111111";
        UserEmailDTO userSinNombre = new UserEmailDTO(1, null, "user@example.com");

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(userId)).thenReturn(userSinNombre);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        when(mapper.toNotificationDTO(any(NotificationLog.class))).thenReturn(
                new NotificationResponseDTO(1, 1, "PASSWORD_RECOVERY", "SENT", LocalDateTime.now())
        );
        doNothing().when(notificationChannel).send(anyString(), anyString(), anyString());

        // When
        notificationService.sendPasswordRecoveryCode(userId, code);

        // Then
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationChannel).send(eq("user@example.com"), anyString(), bodyCaptor.capture());

        String processedBody = bodyCaptor.getValue();
        assertTrue(processedBody.contains("Usuario"), "Debe usar 'Usuario' como nombre por defecto cuando fullName es null");
        assertTrue(processedBody.contains(code), "Debe contener el código");
    }

    @Test
    @DisplayName("Debería procesar múltiples códigos diferentes correctamente")
    void deberiaProcesarMultiplesCodigosDiferentesCorrectamente() throws Exception {
        // Given
        Integer userId = 1;
        String[] codes = {"111111", "222222", "333333"};

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(userId)).thenReturn(mockUserEmail);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        when(mapper.toNotificationDTO(any(NotificationLog.class))).thenReturn(
                new NotificationResponseDTO(1, 1, "PASSWORD_RECOVERY", "SENT", LocalDateTime.now())
        );
        doNothing().when(notificationChannel).send(anyString(), anyString(), anyString());

        // When & Then
        for (String code : codes) {
            notificationService.sendPasswordRecoveryCode(userId, code);

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationChannel, atLeastOnce()).send(anyString(), anyString(), bodyCaptor.capture());

            String lastBody = bodyCaptor.getValue();
            assertTrue(lastBody.contains(code), "El body debe contener el código específico: " + code);
        }
    }

    @Test
    @DisplayName("Debería usar canal EMAIL por defecto")
    void deberiaUsarCanalEmailPorDefecto() throws Exception {
        // Given
        Integer userId = 1;
        String code = "999999";

        when(templateRepository.findByCode("PASSWORD_RECOVERY")).thenReturn(Optional.of(mockTemplate));
        when(userClient.getUserEmailById(userId)).thenReturn(mockUserEmail);
        when(notificationFactory.createChannel("EMAIL")).thenReturn(notificationChannel);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        when(mapper.toNotificationDTO(any(NotificationLog.class))).thenReturn(
                new NotificationResponseDTO(1, 1, "PASSWORD_RECOVERY", "SENT", LocalDateTime.now())
        );
        doNothing().when(notificationChannel).send(anyString(), anyString(), anyString());

        // When
        notificationService.sendPasswordRecoveryCode(userId, code);

        // Then
        verify(notificationFactory, times(1)).createChannel("EMAIL");
        verify(notificationChannel, times(1)).send(anyString(), anyString(), anyString());
    }
}
