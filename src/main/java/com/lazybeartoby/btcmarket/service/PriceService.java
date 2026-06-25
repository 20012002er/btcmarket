package com.lazybeartoby.btcmarket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lazybeartoby.btcmarket.common.constant.AppConstants;
import com.lazybeartoby.btcmarket.model.entity.PriceTick;
import com.lazybeartoby.btcmarket.model.vo.PriceVO;
import com.lazybeartoby.btcmarket.repository.PriceTickRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PriceService {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);
    private static final String BINANCE_24H_URL = "https://api.binance.com/api/v3/ticker/24hr?symbol=BTCUSDT";

    private final PriceTickRepository priceTickRepository;
    private final RestClient restClient;

    @Value("${app.price.fallback-simulated:true}")
    private boolean fallbackSimulated;

    private final AtomicReference<PriceVO> latest = new AtomicReference<>();
    private BigDecimal simulatedPrice = new BigDecimal("107000.00");

    public PriceService(PriceTickRepository priceTickRepository) {
        this.priceTickRepository = priceTickRepository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restClient = RestClient.builder()
                .baseUrl(BINANCE_24H_URL)
                .requestFactory(factory)
                .build();

        // 从已有历史数据中恢复最新价格，避免模拟价格与实际脱节
        try {
            List<PriceTick> recent = priceTickRepository.findByAssetSymbolOrderByTimestampDesc(
                    AppConstants.ASSET_BTC,
                    org.springframework.data.domain.PageRequest.of(0, 1)
            );
            if (!recent.isEmpty()) {
                simulatedPrice = recent.get(0).getPrice();
                log.info("从历史数据恢复最新价格: {}", simulatedPrice);
            }
        } catch (Exception e) {
            log.warn("恢复历史价格失败: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.price.fetch-interval-ms:3000}")
    public void fetchPrice() {
        try {
            JsonNode node = restClient.get().retrieve().body(JsonNode.class);
            if (node != null && node.has("lastPrice")) {
                BigDecimal price = new BigDecimal(node.get("lastPrice").asText());
                BigDecimal change = node.has("priceChangePercent")
                        ? new BigDecimal(node.get("priceChangePercent").asText())
                        : BigDecimal.ZERO;

                PriceVO vo = new PriceVO();
                vo.setAssetSymbol(AppConstants.ASSET_BTC);
                vo.setPrice(price);
                vo.setChange24h(change);
                vo.setTimestamp(System.currentTimeMillis());
                vo.setSource("binance");
                latest.set(vo);

                persistTick(price, "binance");
                simulatedPrice = price;
                return;
            }
        } catch (Exception e) {
            log.warn("Binance 价格获取失败，使用模拟价格: {}", e.getMessage());
        }
        if (fallbackSimulated) {
            simulatePrice();
        }
    }

    private void simulatePrice() {
        BigDecimal delta = simulatedPrice.multiply(new BigDecimal("0.0008"));
        BigDecimal move = delta.multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(-1, 1)))
                .setScale(2, RoundingMode.HALF_UP);
        simulatedPrice = simulatedPrice.add(move).setScale(2, RoundingMode.HALF_UP);

        PriceVO vo = new PriceVO();
        vo.setAssetSymbol(AppConstants.ASSET_BTC);
        vo.setPrice(simulatedPrice);
        vo.setChange24h(BigDecimal.ZERO);
        vo.setTimestamp(System.currentTimeMillis());
        vo.setSource("simulated");
        latest.set(vo);

        persistTick(simulatedPrice, "simulated");
    }

    private void persistTick(BigDecimal price, String source) {
        PriceTick tick = new PriceTick();
        tick.setAssetSymbol(AppConstants.ASSET_BTC);
        tick.setPrice(price);
        tick.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
        tick.setSource(source);
        priceTickRepository.save(tick);
    }

    public PriceVO getCurrentPrice() {
        PriceVO vo = latest.get();
        if (vo == null) {
            fetchPrice();
            vo = latest.get();
        }
        return vo;
    }

    public BigDecimal getLatestPrice() {
        PriceVO vo = getCurrentPrice();
        return vo != null ? vo.getPrice() : simulatedPrice;
    }

    public List<PriceTick> recentTicks(int limit) {
        return priceTickRepository.findByAssetSymbolOrderByTimestampDesc(
                AppConstants.ASSET_BTC,
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }
}
