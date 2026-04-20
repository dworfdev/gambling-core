package com.chroma.gambling.gambling_core.service;

import com.chroma.gambling.gambling_core.entity.Player;
import com.chroma.gambling.gambling_core.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ServerService
{
    @Autowired
    private PlayerRepository playerRepository;

    @Transactional
    public Player deposit(Player player, BigDecimal amount)
    {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        player.setBalance(player.getBalance().add(amount));
        return playerRepository.save(player);
    }

    @Transactional
    public Player cashout(Player player, BigDecimal amount)
    {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        if (player.getBalance().compareTo(amount) < 0)
        {
            throw new RuntimeException("Insufficient funds");
        }

        player.setBalance(player.getBalance().subtract(amount));
        return playerRepository.save(player);
    }
}
