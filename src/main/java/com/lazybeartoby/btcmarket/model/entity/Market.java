package com.lazybeartoby.btcmarket.model.entity;

import com.lazybeartoby.btcmarket.common.constant.MarketResult;
import com.lazybeartoby.btcmarket.common.constant.MarketStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_market")
public class Market {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "market_no", unique = true, length = 32)
    private String marketNo;

    @Column(length = 128)
    private String title;

    @Column(name = "asset_symbol", length = 16)
    private String assetSymbol;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "open_price", precision = 24, scale = 8)
    private BigDecimal openPrice;

    @Column(name = "close_price", precision = 24, scale = 8)
    private BigDecimal closePrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private MarketResult result = MarketResult.PENDING;

    @Column(name = "total_bet_up", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBetUp = BigDecimal.ZERO;

    @Column(name = "total_bet_down", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBetDown = BigDecimal.ZERO;

    @Column(name = "bet_count", nullable = false)
    private Integer betCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketStatus status = MarketStatus.PENDING;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getMarketNo() { return marketNo; }
    public void setMarketNo(String marketNo) { this.marketNo = marketNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }
    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }
    public MarketResult getResult() { return result; }
    public void setResult(MarketResult result) { this.result = result; }
    public BigDecimal getTotalBetUp() { return totalBetUp; }
    public void setTotalBetUp(BigDecimal totalBetUp) { this.totalBetUp = totalBetUp; }
    public BigDecimal getTotalBetDown() { return totalBetDown; }
    public void setTotalBetDown(BigDecimal totalBetDown) { this.totalBetDown = totalBetDown; }
    public Integer getBetCount() { return betCount; }
    public void setBetCount(Integer betCount) { this.betCount = betCount; }
    public MarketStatus getStatus() { return status; }
    public void setStatus(MarketStatus status) { this.status = status; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
