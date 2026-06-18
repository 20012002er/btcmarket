package com.lazybeartoby.btcmarket.model.entity;

import com.lazybeartoby.btcmarket.common.constant.BetDirection;
import com.lazybeartoby.btcmarket.common.constant.BetResult;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_bet")
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bet_no", unique = true, length = 32)
    private String betNo;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private BetDirection direction;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal odds = BigDecimal.ONE;

    @Column(name = "win_amount", precision = 18, scale = 2)
    private BigDecimal winAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private BetResult result = BetResult.PENDING;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBetNo() { return betNo; }
    public void setBetNo(String betNo) { this.betNo = betNo; }
    public Long getMarketId() { return marketId; }
    public void setMarketId(Long marketId) { this.marketId = marketId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BetDirection getDirection() { return direction; }
    public void setDirection(BetDirection direction) { this.direction = direction; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
    public BigDecimal getWinAmount() { return winAmount; }
    public void setWinAmount(BigDecimal winAmount) { this.winAmount = winAmount; }
    public BetResult getResult() { return result; }
    public void setResult(BetResult result) { this.result = result; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
}
