package com.chroma.gambling.gambling_core.service;

import com.chroma.gambling.gambling_core.entity.Player;
import com.chroma.gambling.gambling_core.entity.Transaction;
import com.chroma.gambling.gambling_core.entity.TransactionStatus;
import com.chroma.gambling.gambling_core.entity.TransactionType;
import com.chroma.gambling.gambling_core.repository.PlayerRepository;
import com.chroma.gambling.gambling_core.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TransactionService
{
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PlayerRepository playerRepository;

    @Transactional
    public String approve(Long transactionId, boolean approve)
    {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId).orElseThrow(() ->
                new RuntimeException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING)
        {
            return "Transaction already processed";
        }

        Player player = playerRepository.findByIdForUpdate(transaction.getTgId()).orElseThrow(() ->
                new RuntimeException("Player not found"));

        if (approve)
        {
            transaction.setStatus(TransactionStatus.SUCCESS);
            if (transaction.getType() == TransactionType.DEPOSIT)
            {
                player.setBalance(player.getBalance().add(transaction.getAmount()));
                player.setTotalDeposited(player.getTotalDeposited().add(transaction.getAmount()));
            }
        }
        else
        {
            transaction.setStatus(TransactionStatus.REJECTED);
            if (transaction.getType() == TransactionType.WITHDRAWAL)
            {
                player.setBalance(player.getBalance().add(transaction.getAmount()));
            }
        }

        playerRepository.save(player);
        transactionRepository.save(transaction);

        return "Status updated to " + transaction.getStatus();
    }
}
