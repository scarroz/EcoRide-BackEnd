package co.edu.unbosque.userservice.controller;

import co.edu.unbosque.userservice.dto.PaymentCardRequestDTO;
import co.edu.unbosque.userservice.dto.WalletNotificationDTO;
import co.edu.unbosque.userservice.dto.WalletRechargeRequestDTO;
import co.edu.unbosque.userservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(summary = "Registrar método de pago del usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Método de pago registrado correctamente")
    })
    @PostMapping("/register-card")
    public ResponseEntity<String> registerCard(@RequestBody PaymentCardRequestDTO request) {
        walletService.registerCard(request);
        return ResponseEntity.ok("Tarjeta registrada correctamente en Stripe.");
    }

    @Operation(summary = "Recargar wallet", description = "Permite recargar saldo en la wallet usando Stripe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recarga procesada correctamente")
    })
    @PostMapping("/recharge")
    public ResponseEntity<String> rechargeWallet(@RequestBody WalletRechargeRequestDTO request) {
        walletService.rechargeWallet(request);
        return ResponseEntity.ok("Recarga procesada correctamente. Verifica tu nuevo saldo.");
    }

    @PostMapping("/notify-recharge")
    public ResponseEntity<Void> notifyRecharge(@RequestBody WalletNotificationDTO notification) {
        walletService.updateWalletBalance(notification);
        return ResponseEntity.ok().build();
    }
}
