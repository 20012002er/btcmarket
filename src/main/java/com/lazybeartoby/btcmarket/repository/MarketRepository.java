package com.lazybeartoby.btcmarket.repository;

import com.lazybeartoby.btcmarket.common.constant.MarketStatus;
import com.lazybeartoby.btcmarket.model.entity.Market;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarketRepository extends JpaRepository<Market, Long> {

    List<Market> findByStatusInOrderByStartTimeAsc(List<MarketStatus> statuses);

    List<Market> findByStatusOrderByEndTimeDesc(MarketStatus status);

    List<Market> findByStatus(MarketStatus status);

    Optional<Market> findByMarketNo(String marketNo);

    @Query("select m from Market m where m.templateId = :templateId and m.startTime = :startTime")
    Optional<Market> findByTemplateAndStartTime(@Param("templateId") Long templateId,
                                                @Param("startTime") LocalDateTime startTime);

    @Query("select m from Market m where m.status = :status and m.startTime <= :now")
    List<Market> findPendingToOpen(@Param("status") MarketStatus status, @Param("now") LocalDateTime now);

    @Query("select m from Market m where m.status = :status and m.endTime <= :now")
    List<Market> findActiveToClose(@Param("status") MarketStatus status, @Param("now") LocalDateTime now);

    Page<Market> findByStatusInOrderByStartTimeDesc(List<MarketStatus> statuses, Pageable pageable);

    List<Market> findTop10ByStatusOrderBySettledAtDesc(MarketStatus status);
}
