package com.chroma.gambling.gambling_core.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "players")
@Data
public class Player
{
    @Id
    private Long tgId;

    @Version
    private Long version;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_deposited")
    private BigDecimal totalDeposited = BigDecimal.ZERO;

    @Column(name = "next_transaction_hash")
    private String nextTransactionHash;

    @Column(name = "wallet_address")
    private String walletAddress;

    @Column(name = "risk_level")
    private double riskLevel = 0.1;

    @Column(name = "is_admin")
    private boolean isAdmin = false;
}