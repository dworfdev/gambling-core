package com.chroma.gambling.gambling_core.controller;

import com.chroma.gambling.gambling_core.entity.Player;
import com.chroma.gambling.gambling_core.entity.Transaction;
import com.chroma.gambling.gambling_core.entity.TransactionStatus;
import com.chroma.gambling.gambling_core.repository.PlayerRepository;
import com.chroma.gambling.gambling_core.repository.TransactionRepository;
import com.chroma.gambling.gambling_core.service.PlayerService;
import com.chroma.gambling.gambling_core.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController
{
    @Autowired private PlayerService playerService;
    @Autowired private TransactionService transactionService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PlayerRepository playerRepository;

    private void checkAdmin(Long tgId)
    {
        Player player = playerService.getPlayer(tgId);

        if (!player.isAdmin())
        {
            throw new RuntimeException("Access denied");
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(@RequestParam Long tgId)
    {
        checkAdmin(tgId);
        return Map.of("totalUsers", playerRepository.countBy());
    }

    @GetMapping("/pending")
    public Page<Transaction> getPendingTransactions(
            @RequestParam Long tgId,
            @RequestParam(required = false) Long targetTgId,
            @RequestParam(defaultValue = "0") int page)
    {
        checkAdmin(tgId);

        Pageable pageable = PageRequest.of(page, 50, Sort.by("createdAt"));

        if (targetTgId != null)
        {
            return transactionRepository.findByTgIdAndStatus(targetTgId, TransactionStatus.PENDING, pageable);
        }

        return transactionRepository.findByStatus(TransactionStatus.PENDING, pageable);
    }

    @PostMapping("/approve")
    public String approveTransaction(
            @RequestParam Long tgId,
            @RequestParam Long transactionId,
            @RequestParam boolean approve)
    {
        checkAdmin(tgId);

        return transactionService.approve(transactionId, approve);
    }
}
