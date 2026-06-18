package com.lazybeartoby.btcmarket.repository;

import com.lazybeartoby.btcmarket.model.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    @Query("select coalesce(sum(w.balance), 0) from Wallet w")
    BigDecimal totalBalance();

    @Query("select count(w) from Wallet w where w.balance > :min")
    long countActive(@Param("min") BigDecimal min);
}
