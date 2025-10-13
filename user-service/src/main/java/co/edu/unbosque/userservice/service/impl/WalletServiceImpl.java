package co.edu.unbosque.userservice.service.impl;

import co.edu.unbosque.userservice.client.PaymentClient;
import co.edu.unbosque.userservice.dto.PaymentCardRequestDTO;
import co.edu.unbosque.userservice.dto.WalletDTO;
import co.edu.unbosque.userservice.dto.WalletNotificationDTO;
import co.edu.unbosque.userservice.dto.WalletRechargeRequestDTO;
import co.edu.unbosque.userservice.exception.ResourceNotFoundException;
import co.edu.unbosque.userservice.model.UserAccount;
import co.edu.unbosque.userservice.model.Wallet;
import co.edu.unbosque.userservice.repository.UserAccountRepository;
import co.edu.unbosque.userservice.repository.WalletRepository;
import co.edu.unbosque.userservice.service.WalletService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WalletServiceImpl implements WalletService {

    private final PaymentClient paymentClient;
    private final WalletRepository walletRepo;
    private final UserAccountRepository userAccountRepo;

    public WalletServiceImpl(PaymentClient paymentClient, WalletRepository walletRepo,UserAccountRepository userAccountRepo) {
        this.paymentClient = paymentClient;
        this.walletRepo = walletRepo;
        this.userAccountRepo = userAccountRepo;
    }

    @Override
    public void registerCard(PaymentCardRequestDTO request) {
        paymentClient.registerCard(request);
    }

    @Override
    public void rechargeWallet(WalletRechargeRequestDTO request) {
        paymentClient.rechargeWallet(request);
    }

    @Override
    public void updateWalletBalance(WalletNotificationDTO notification) {
        Wallet wallet = walletRepo.findByUserId(notification.userId())
                .orElseThrow(() -> new RuntimeException("No se encontró una wallet para el usuario con ID: " + notification.userId()));

        wallet.setBalance(wallet.getBalance().add(notification.amount()));
        wallet.setLastUpdated(java.time.LocalDateTime.now());
        walletRepo.save(wallet);

        System.out.println("Wallet actualizada correctamente para el usuario " + notification.userId() +
                " con nuevo saldo: " + wallet.getBalance());
    }

    @Override
    public WalletDTO getWalletByUserId(Long userId) {
        return null;
    }

}
