package com.chroma.gambling.gambling_core.controller;

import com.chroma.gambling.gambling_core.entity.Player;
import com.chroma.gambling.gambling_core.repository.PlayerRepository;
import com.chroma.gambling.gambling_core.service.HashService;
import com.chroma.gambling.gambling_core.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController
{
    @Autowired
    private PlayerService playerService;

    @Autowired
    private HashService hashController;

    @Autowired
    private PlayerRepository playerRepository;

    @GetMapping(value = "/prepare", produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> prepareTransaction(@RequestParam Long id)
    {
        String newHash = java.util.UUID.randomUUID().toString();

        Player player = playerService.getPlayer(id);
        hashController.updateNextHash(player, newHash);

        return ResponseEntity.ok(newHash);
    }

    record DepositRequest(Long id, BigDecimal amount, String hash) {}

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody DepositRequest req)
    {
        Player player = playerService.getPlayer(req.id);
        playerService.requestDeposit(player, req.amount, req.hash);

        return ResponseEntity.ok("Deposit request received successfully");
    }

    record CashoutRequest(Long id, BigDecimal amount, String hash) {}

    @PostMapping("/cashout")
    public ResponseEntity<String> cashout(@RequestBody CashoutRequest req)
    {
        Player player = playerService.getPlayer(req.id);
        playerService.requestCashout(player, req.amount, req.hash);

        return ResponseEntity.ok("Cashout request received successfully");
    }

    record BindWalletRequest(Long id, String address) {}

    @PostMapping("/player/bind-wallet")
    public ResponseEntity<?> bindWallet(@RequestBody BindWalletRequest req)
    {
        Player player = playerService.getPlayer(req.id);
        player.setWalletAddress(req.address);

        playerRepository.save(player);

        return ResponseEntity.ok("Wallet connected to account successfully");
    }

    @PostMapping("/invalidate")
    public ResponseEntity<Void> invalidateTransaction(@RequestParam Long id)
    {
        Player player = playerService.getPlayer(id);
        hashController.invalidateHash(player);

        return ResponseEntity.ok().build();
    }
}