package com.lazybeartoby.btcmarket.model.vo;

import com.lazybeartoby.btcmarket.common.constant.BetDirection;
import com.lazybeartoby.btcmarket.common.constant.BetResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BetVO {

    private Long id;
    private String betNo;
    private Long marketId;
    private String marketTitle;
    private String username;
    private String assetSymbol;
    private BetDirection direction;
    private BigDecimal amount;
    private BigDecimal odds;
    private BigDecimal winAmount;
    private BetResult result;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBetNo() { return betNo; }
    public void setBetNo(String betNo) { this.betNo = betNo; }
    public Long getMarketId() { return marketId; }
    public void setMarketId(Long marketId) { this.marketId = marketId; }
    public String getMarketTitle() { return marketTitle; }
    public void setMarketTitle(String marketTitle) { this.marketTitle = marketTitle; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
}
