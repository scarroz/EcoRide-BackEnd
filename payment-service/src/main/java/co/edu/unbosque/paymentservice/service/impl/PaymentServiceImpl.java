package co.edu.unbosque.paymentservice.service.impl;

import co.edu.unbosque.paymentservice.client.UserNotificationClient;
import co.edu.unbosque.paymentservice.dto.*;
import co.edu.unbosque.paymentservice.mapper.DataMapper;
import co.edu.unbosque.paymentservice.model.PaymentMethod;
import co.edu.unbosque.paymentservice.model.Transaction;
import co.edu.unbosque.paymentservice.repository.PaymentMethodRepository;
import co.edu.unbosque.paymentservice.repository.TransactionRepository;
import co.edu.unbosque.paymentservice.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;
    private final UserNotificationClient userNotificationClient;
    private final DataMapper mapper;

    public PaymentServiceImpl(
            PaymentMethodRepository paymentMethodRepository,
            TransactionRepository transactionRepository,
            UserNotificationClient userNotificationClient,
            DataMapper mapper
    ) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
        this.userNotificationClient = userNotificationClient;
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    @Transactional
    public PaymentMethodResponseDTO registerCard(PaymentCardRequestDTO request) {
        try {
            System.out.println("Registrando nueva tarjeta para usuario: " + request.userId());

            // Buscar o crear cliente en Stripe
            String customerId = paymentMethodRepository
                    .findByUserIdAndActiveTrue(request.userId())
                    .stream()
                    .findFirst()
                    .map(PaymentMethod::getStripeCustomerId)
                    .orElseGet(() -> createStripeCustomer(request.name(), request.email()));

            // Obtener y vincular metodo de pago en Stripe
            com.stripe.model.PaymentMethod stripeMethod =
                    com.stripe.model.PaymentMethod.retrieve(request.paymentMethodId());

            stripeMethod.attach(PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build());

            System.out.println(" Método de pago vinculado a Stripe: " + stripeMethod.getId());

            // USAR MAPPER para crear la entidad
            PaymentMethod method = mapper.toPaymentMethodEntity(
                    request.userId(),
                    customerId,
                    stripeMethod.getId(),
                    stripeMethod.getCard().getBrand(),
                    stripeMethod.getCard().getLast4(),
                    "CARD"
            );

            paymentMethodRepository.save(method);
            System.out.println("Método de pago guardado en BD con ID: " + method.getId());

            // USAR MAPPER para crear el DTO de respuesta
            return mapper.toPaymentMethodResponseDTO(method);

        } catch (StripeException e) {
            System.err.println("Error Stripe: " + e.getMessage());
            throw new RuntimeException("Error al registrar tarjeta: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public TransactionResponseDTO rechargeWallet(WalletRechargeRequestDTO request) {
        Transaction transaction = null;

        try {
            System.out.println("Procesando recarga de wallet...");
            System.out.println("   Usuario: " + request.userId());
            System.out.println("   Monto: $" + request.amount());

            if (request.paymentMethodId() == null || request.paymentMethodId().isBlank()) {
                throw new RuntimeException("El ID del método de pago es obligatorio");
            }

            PaymentMethod paymentMethod = paymentMethodRepository
                    .findByTokenId(request.paymentMethodId())
                    .orElseThrow(() -> new RuntimeException(
                            "Método de pago no encontrado: " + request.paymentMethodId()
                    ));

            validatePaymentMethod(paymentMethod, request.userId());

            transaction = mapper.createWalletRechargeTransaction(
                    request.userId(),
                    paymentMethod.getId(),
                    request.amount()
            );

            transaction = transactionRepository.save(transaction);
            System.out.println("Transacción registrada con ID: " + transaction.getId());

            PaymentIntent intent = createStripePaymentIntent(
                    paymentMethod,
                    request.amount(),
                    transaction.getId(),
                    "Recarga de wallet - Usuario: " + request.userId()
            );

            updateTransactionWithStripeResult(transaction, intent);

            if ("COMPLETED".equals(transaction.getStatus())) {
                notifyWalletRecharge(request.userId(), request.amount());
            }

            return mapper.toTransactionResponseDTO(transaction);

        } catch (StripeException e) {
            System.err.println("Error Stripe: " + e.getMessage());
            markTransactionAsFailed(transaction);
            throw new RuntimeException("Error procesando pago: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Error general: " + e.getMessage());
            markTransactionAsFailed(transaction);
            throw new RuntimeException("Error procesando recarga: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PaymentMethodResponseDTO> getActiveMethodsByUser(Long userId) {
        System.out.println("Obteniendo métodos de pago para usuario: " + userId);

        // USAR MAPPER para convertir lista de entidades a DTOs
        List<PaymentMethodResponseDTO> methods = paymentMethodRepository
                .findByUserIdAndActiveTrue(userId)
                .stream()
                .map(mapper::toPaymentMethodResponseDTO)
                .collect(Collectors.toList());

        System.out.println("Encontrados " + methods.size() + " métodos de pago");
        return methods;
    }

    @Override
    public List<TransactionResponseDTO> getTransactionsByUser(Long userId) {
        System.out.println("Obteniendo transacciones para usuario: " + userId);

        // USAR MAPPER para convertir lista de transacciones
        List<TransactionResponseDTO> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toTransactionResponseDTO)
                .collect(Collectors.toList());

        System.out.println("Encontradas " + transactions.size() + " transacciones");
        return transactions;
    }

    @Override
    public TransactionDetailDTO getTransactionDetail(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));

        // Buscar método de pago si existe
        PaymentMethod paymentMethod = null;
        if (transaction.getPaymentMethodId() != null) {
            paymentMethod = paymentMethodRepository
                    .findById(transaction.getPaymentMethodId())
                    .orElse(null);
        }

        //USAR MAPPER para crear DTO detallado
        return mapper.toTransactionDetailDTO(transaction, paymentMethod);
    }

    // ========================================
    // MÉTODOS AUXILIARES (PRIVADOS)
    // ========================================

    private String createStripeCustomer(String name, String email) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setName(name)
                    .setEmail(email)
                    .build();
            Customer customer = Customer.create(params);
            System.out.println("Cliente Stripe creado: " + customer.getId());
            return customer.getId();
        } catch (StripeException e) {
            throw new RuntimeException("Error creando cliente en Stripe: " + e.getMessage());
        }
    }

    private void validatePaymentMethod(PaymentMethod paymentMethod, Long userId) {
        if (!paymentMethod.getUserId().equals(userId)) {
            throw new RuntimeException("El método de pago no pertenece al usuario");
        }
        if (!paymentMethod.isActive()) {
            throw new RuntimeException("El método de pago está inactivo");
        }
    }

    private PaymentIntent createStripePaymentIntent(
            PaymentMethod paymentMethod,
            BigDecimal amount,
            Long transactionId,
            String description
    ) throws StripeException {
        long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("cop")
                .setCustomer(paymentMethod.getStripeCustomerId())
                .setPaymentMethod(paymentMethod.getTokenId())
                .setConfirm(true)
                .setOffSession(true)
                .setDescription(description)
                .putMetadata("transactionId", transactionId.toString())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        System.out.println("PaymentIntent creado: " + intent.getId() + " - Status: " + intent.getStatus());

        return intent;
    }

    private void updateTransactionWithStripeResult(Transaction transaction, PaymentIntent intent) {
        transaction.setStripePaymentId(intent.getId());

        switch (intent.getStatus()) {
            case "succeeded" -> {
                transaction.setStatus("COMPLETED");
                System.out.println("Pago completado exitosamente");
            }
            case "requires_action", "processing" -> {
                transaction.setStatus("PROCESSING");
                System.out.println("Pago en proceso");
            }
            default -> {
                transaction.setStatus("FAILED");
                System.err.println("Pago fallido con status: " + intent.getStatus());
                throw new RuntimeException("El pago no fue exitoso. Status: " + intent.getStatus());
            }
        }

        transactionRepository.save(transaction);
    }

    private void markTransactionAsFailed(Transaction transaction) {
        if (transaction != null) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            System.out.println("Transacción marcada como FAILED");
        }
    }

    private void notifyWalletRecharge(Long userId, BigDecimal amount) {
        try {
            // USAR MAPPER para crear notificación
            WalletNotificationDTO notification = mapper.toWalletNotificationDTO(userId, amount);
            userNotificationClient.notifyWalletRecharge(notification);
            System.out.println("Notificación enviada al UserService");
        } catch (Exception e) {
            System.err.println("Error notificando (el pago SÍ se completó): " + e.getMessage());
        }
    }
    @Override
    public List<TransactionResponseDTO> getTransactionsByUserAndStatus(Long userId, String status) {
        return transactionRepository
                .findByUserIdAndStatus(userId, status)
                .stream()
                .map(mapper::toTransactionResponseDTO)
                .collect(Collectors.toList());
    }
}