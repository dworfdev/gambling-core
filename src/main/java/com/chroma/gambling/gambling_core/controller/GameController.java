package com.chroma.gambling.gambling_core.controller;

import com.chroma.gambling.gambling_core.entity.Player;
import com.chroma.gambling.gambling_core.repository.PlayerRepository;
import com.chroma.gambling.gambling_core.service.PlayerService;
import com.chroma.gambling.gambling_core.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Locale;

@RestController
@RequestMapping("/api/games")
public class GameController
{
    @Autowired private PlayerRepository playerRepository;
    @Autowired private ServerService serverService;
    @Autowired private PlayerService playerService;

    private static final SecureRandom rng = new SecureRandom();

    @PostMapping("/dice")
    @Transactional
    public String playDice(@RequestParam Long id, @RequestParam BigDecimal bet)
    {
        Player player = playerService.getPlayerForUpdate(id);
        serverService.cashout(player, bet);

        double risk = player.getRiskLevel();

        double greenChance  = 0.375 + (0.05 * (0.5 - risk));
        double yellowChance = 0.0417;

        double roll = rng.nextDouble();
        int sector;
        String resultType;
        BigDecimal winAmount = BigDecimal.ZERO;

        if (roll < yellowChance)
        {
            sector = 0;
            resultType = "yellow";
            winAmount = bet.multiply(new BigDecimal("5.00"));
            serverService.deposit(player, winAmount);
            player.setRiskLevel(Math.min(1.0, risk + 0.05));
        }
        else if (roll < yellowChance + greenChance)
        {
            int[] greenSectors = {1,3,5,7,9,11,13,15,17,19};
            sector = greenSectors[rng.nextInt(greenSectors.length)];
            resultType = "green";
            winAmount = bet.multiply(new BigDecimal("2.50"));
            serverService.deposit(player, winAmount);
            player.setRiskLevel(Math.min(1.0, risk + 0.03));
        }
        else
        {
            int[] redSectors = {2,4,6,8,10,12,14,16,18};
            sector = redSectors[rng.nextInt(redSectors.length)];
            resultType = "red";
            player.setRiskLevel(Math.max(0.0, risk - 0.03));
        }

        playerRepository.save(player);

        return String.format(Locale.US, "%d|%s|%.2f|%.2f",
                sector, resultType, winAmount, player.getRiskLevel());
    }
}