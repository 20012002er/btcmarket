package com.lazybeartoby.btcmarket.model.vo;

import java.math.BigDecimal;

public class PriceVO {
    private String assetSymbol;
    private BigDecimal price;
    private BigDecimal change24h;
    private Long timestamp;
    private String source;

    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getChange24h() { return change24h; }
    public void setChange24h(BigDecimal change24h) { this.change24h = change24h; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
