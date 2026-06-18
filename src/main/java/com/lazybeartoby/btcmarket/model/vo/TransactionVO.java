package com.lazybeartoby.btcmarket.model.vo;

import com.lazybeartoby.btcmarket.common.constant.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionVO {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private Integer direction;
    private BigDecimal balanceAfter;
    private BigDecimal frozenAfter;
    private String remark;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getDirection() { return direction; }
    public void setDirection(Integer direction) { this.direction = direction; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public BigDecimal getFrozenAfter() { return frozenAfter; }
    public void setFrozenAfter(BigDecimal frozenAfter) { this.frozenAfter = frozenAfter; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
