package com.lazybeartoby.btcmarket.repository;

import com.lazybeartoby.btcmarket.model.entity.Bet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BetRepository extends JpaRepository<Bet, Long> {

    Page<Bet> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Bet> findByMarketIdOrderByCreatedAtDesc(Long marketId);

    List<Bet> findByMarketIdAndResult(Long marketId, com.lazybeartoby.btcmarket.common.constant.BetResult result);

    List<Bet> findByMarketIdAndStatus(Long marketId, Integer status);

    @Query("select coalesce(sum(b.amount), 0) from Bet b where b.userId = :userId and b.status = 1")
    BigDecimal sumActiveAmountByUser(@Param("userId") Long userId);

    @Query("select coalesce(sum(b.winAmount), 0) - coalesce(sum(case when b.result = com.lazybeartoby.btcmarket.common.constant.BetResult.LOSS then b.amount else 0 end), 0) from Bet b where b.userId = :userId and b.result <> com.lazybeartoby.btcmarket.common.constant.BetResult.PENDING")
    BigDecimal profitByUser(@Param("userId") Long userId);

    long countByUserIdAndResult(Long userId, com.lazybeartoby.btcmarket.common.constant.BetResult result);

    long countByUserId(Long userId);
}
