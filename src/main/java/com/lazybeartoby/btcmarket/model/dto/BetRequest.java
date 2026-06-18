package com.lazybeartoby.btcmarket.model.dto;

import com.lazybeartoby.btcmarket.common.constant.BetDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BetRequest {

    @NotNull(message = "市场ID不能为空")
    private Long marketId;

    @NotNull(message = "押注方向不能为空")
    private BetDirection direction;

    @NotNull(message = "押注金额不能为空")
    @DecimalMin(value = "1", message = "最小押注1积分")
    private BigDecimal amount;

    public Long getMarketId() { return marketId; }
    public void setMarketId(Long marketId) { this.marketId = marketId; }
    public BetDirection getDirection() { return direction; }
    public void setDirection(BetDirection direction) { this.direction = direction; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
