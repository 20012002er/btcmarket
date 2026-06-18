package com.lazybeartoby.btcmarket.repository;

import com.lazybeartoby.btcmarket.model.entity.PriceTick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceTickRepository extends JpaRepository<PriceTick, Long> {
    List<PriceTick> findTop60ByAssetSymbolOrderByTimestampDesc(String assetSymbol);
}
