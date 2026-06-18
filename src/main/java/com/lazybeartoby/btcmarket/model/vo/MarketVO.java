package com.lazybeartoby.btcmarket.model.vo;

import com.lazybeartoby.btcmarket.common.constant.MarketResult;
import com.lazybeartoby.btcmarket.common.constant.MarketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketVO {

    private Long id;
    private String marketNo;
    private String title;
    private String assetSymbol;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private MarketResult result;
    private BigDecimal totalBetUp;
    private BigDecimal totalBetDown;
    private Integer betCount;
    private MarketStatus status;
    private BigDecimal oddsUp;
    private BigDecimal oddsDown;
    private Long countdownSeconds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public BigDecimal getOddsUp() { return oddsUp; }
    public void setOddsUp(BigDecimal oddsUp) { this.oddsUp = oddsUp; }
    public BigDecimal getOddsDown() { return oddsDown; }
    public void setOddsDown(BigDecimal oddsDown) { this.oddsDown = oddsDown; }
    public Long getCountdownSeconds() { return countdownSeconds; }
    public void setCountdownSeconds(Long countdownSeconds) { this.countdownSeconds = countdownSeconds; }
}
