package com.chroma.gambling.gambling_core.service;

import com.chroma.gambling.gambling_core.entity.Player;
import com.chroma.gambling.gambling_core.entity.TransactionStatus;
import com.chroma.gambling.gambling_core.entity.TransactionType;
import com.chroma.gambling.gambling_core.repository.PlayerRepository;
import com.chroma.gambling.gambling_core.repository.TransactionRepository;
import com.chroma.gambling.gambling_core.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PlayerService
{
    @Autowired private PlayerRepository playerRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private HashService hashController;

    @Transactional
    public Player createPlayer(Long tgId)
    {
        if (playerRepository.existsById(tgId))
        {
            throw new RuntimeException("User already exists");
        }
        Player player = new Player();
        player.setTgId(tgId);
        return playerRepository.save(player);
    }

    public Player getPlayer(Long id)
    {
        return playerRepository.findById(id).orElseGet(() -> {
            Player player = new Player();
            player.setTgId(id);

            return playerRepository.save(player);
        });
    }

    public Player getPlayerForUpdate(Long id)
    {
        return playerRepository.findByIdForUpdate(id).orElseThrow(() ->
                new RuntimeException("User not found"));
    }

    @Transactional
    public void requestDeposit(Player player, BigDecimal amount, String hash)
    {
        if (player.getNextTransactionHash() == null ||
                player.getNextTransactionHash().startsWith("EXPIRED_") ||
                !player.getNextTransactionHash().equals(hash))
        {
            throw new RuntimeException("Invalid or expired hash");
        }

        hashController.invalidateHash(player);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        playerRepository.save(player);

        createTransactionRecord(
                player.getTgId(),
                amount,
                TransactionType.DEPOSIT,
                TransactionStatus.PENDING,
                player.getWalletAddress());
    }

    @Transactional
    public void requestCashout(Player player, BigDecimal amount, String hash)
    {
        if (player.getNextTransactionHash() == null ||
                player.getNextTransactionHash().startsWith("EXPIRED_") ||
                !player.getNextTransactionHash().equals(hash))
        {
            throw new RuntimeException("Invalid or expired hash");
        }

        hashController.invalidateHash(player);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        if (player.getBalance().compareTo(amount) < 0)
        {
            throw new RuntimeException("Insufficient funds");
        }

        player.setBalance(player.getBalance().subtract(amount));

        playerRepository.save(player);

        createTransactionRecord(
                player.getTgId(),
                amount,
                TransactionType.WITHDRAWAL,
                TransactionStatus.PENDING,
                player.getWalletAddress());
    }

    @Transactional
    Transaction createTransactionRecord(Long tgId, BigDecimal amount,
                                        TransactionType type, TransactionStatus status, String wallet)
    {
        Transaction t = new Transaction();
        t.setTgId(tgId);
        t.setAmount(amount);
        t.setWalletAddress(wallet);
        t.setType(type);
        t.setStatus(status);

        return transactionRepository.save(t);
    }
}

