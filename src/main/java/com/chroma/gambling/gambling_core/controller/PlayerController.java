package com.chroma.gambling.gambling_core.controller;

import com.chroma.gambling.gambling_core.entity.Player;
import com.chroma.gambling.gambling_core.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

record PlayerDto(Long tgId, BigDecimal balance, String walletAddress, boolean isAdmin) {}

@RestController
@RequestMapping("/api/players")
public class PlayerController
{
    @Autowired private PlayerService playerService;

    @PostMapping
    public PlayerDto createPlayer(@RequestParam Long tgId)
    {
        Player p = playerService.createPlayer(tgId);
        return new PlayerDto(p.getTgId(), p.getBalance(), p.getWalletAddress(), p.isAdmin());
    }

    @GetMapping("/{id}")
    public PlayerDto getPlayer(@PathVariable Long id)
    {
        Player p = playerService.getPlayer(id);
        return new PlayerDto(p.getTgId(), p.getBalance(), p.getWalletAddress(), p.isAdmin());
    }
}