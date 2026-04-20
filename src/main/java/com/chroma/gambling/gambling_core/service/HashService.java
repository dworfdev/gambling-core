package com.chroma.gambling.gambling_core.service;

import com.chroma.gambling.gambling_core.entity.Player;
import com.chroma.gambling.gambling_core.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HashService
{
    @Autowired
    private PlayerRepository playerRepository;

    @Transactional
    public void updateNextHash(Player player, String newHash)
    {
        player.setNextTransactionHash(newHash);
        playerRepository.save(player);
    }

    @Transactional
    public void invalidateHash(Player player)
    {
        player.setNextTransactionHash("EXPIRED_" + System.currentTimeMillis());
        playerRepository.saveAndFlush(player);
    }
}
