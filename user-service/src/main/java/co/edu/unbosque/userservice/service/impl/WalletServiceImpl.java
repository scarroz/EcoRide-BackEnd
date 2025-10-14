package co.edu.unbosque.userservice.service.impl;

import co.edu.unbosque.userservice.client.PaymentClient;
import co.edu.unbosque.userservice.dto.*;
import co.edu.unbosque.userservice.exception.ResourceNotFoundException;
import co.edu.unbosque.userservice.mapper.DataMapper;
import co.edu.unbosque.userservice.model.UserAccount;
import co.edu.unbosque.userservice.model.Wallet;
import co.edu.unbosque.userservice.repository.UserAccountRepository;
import co.edu.unbosque.userservice.repository.WalletRepository;
import co.edu.unbosque.userservice.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementacion del servicio de gestion de wallets
 */
@Service
public class WalletServiceImpl implements WalletService {

    private final PaymentClient paymentClient;
    private final WalletRepository walletRepo;
    private final UserAccountRepository userAccountRepo;
    private final DataMapper mapper;

    public WalletServiceImpl(
            PaymentClient paymentClient,
            WalletRepository walletRepo,
            UserAccountRepository userAccountRepo,
            DataMapper mapper
    ) {
        this.paymentClient = paymentClient;
        this.walletRepo = walletRepo;
        this.userAccountRepo = userAccountRepo;
        this.mapper = mapper;
    }

    @Override
    public void registerCard(PaymentCardRequestDTO request) {
        System.out.println("Registrando tarjeta para usuario: " + request.userId());

        // Validar que el usuario existe
        UserAccount user = userAccountRepo.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Delegar al Payment-Service
        paymentClient.registerCard(request);

        System.out.println("Tarjeta registrada exitosamente en Payment-Service");
    }

    @Override
    @Transactional
    public WalletRechargeResponseDTO rechargeWallet(WalletRechargeRequestDTO request) {
        System.out.println("Procesando recarga de wallet para usuario: " + request.userId());

        // Validar que el usuario existe
        UserAccount user = userAccountRepo.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar que la wallet existe
        Wallet wallet = walletRepo.findByUserId(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro wallet para el usuario"
                ));

        // Validar monto minimo
        if (request.amount().doubleValue() < 1.0) {
            throw new IllegalArgumentException("El monto minimo de recarga es $1.00");
        }

        System.out.println("Saldo actual: " + wallet.getBalance());
        System.out.println("Monto a recargar: " + request.amount());

        // Delegar el pago al Payment-Service
        // El Payment-Service procesara con Stripe y notificara de vuelta
        paymentClient.rechargeWallet(request);

        System.out.println("Solicitud de recarga enviada a Payment-Service");

        // Retornar respuesta (el saldo se actualiza cuando Payment-Service notifique)
        return new WalletRechargeResponseDTO(
                "Recarga en proceso. El saldo se actualizara una vez confirmado el pago.",
                wallet.getBalance(),
                request.amount(),
                "pending"
        );
    }

    @Override
    @Transactional
    public void updateWalletBalance(WalletNotificationDTO notification) {
        System.out.println("Recibida notificacion de recarga para usuario: " + notification.userId());
        System.out.println("Monto a agregar: " + notification.amount());

        Wallet wallet = walletRepo.findByUserId(notification.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro wallet para el usuario con ID: " + notification.userId()
                ));

        // Actualizar saldo
        wallet.setBalance(wallet.getBalance().add(notification.amount()));
        wallet.setLastUpdated(LocalDateTime.now());
        walletRepo.save(wallet);

        System.out.println("Wallet actualizada correctamente");
        System.out.println("Nuevo saldo: " + wallet.getBalance());
    }

    @Override
    public WalletDTO getWalletByUserId(Long userId) {
        System.out.println("Obteniendo wallet de usuario: " + userId);

        Wallet wallet = walletRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet no encontrada"));

        return mapper.toWalletDTO(wallet);
    }
}