package com.lazybeartoby.btcmarket.repository;

import com.lazybeartoby.btcmarket.model.entity.MarketTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketTemplateRepository extends JpaRepository<MarketTemplate, Long> {
    List<MarketTemplate> findByStatus(Integer status);
}
