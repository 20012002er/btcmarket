package com.lazybeartoby.btcmarket.service;

import com.lazybeartoby.btcmarket.common.constant.MarketResult;
import com.lazybeartoby.btcmarket.common.constant.MarketStatus;
import com.lazybeartoby.btcmarket.model.entity.Market;
import com.lazybeartoby.btcmarket.model.entity.MarketTemplate;
import com.lazybeartoby.btcmarket.repository.MarketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MarketSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(MarketSchedulerService.class);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final MarketRepository marketRepository;
    private final MarketService marketService;
    private final PriceService priceService;
    private final SettlementService settlementService;

    @Value("${app.market.create-ahead-minutes:10}")
    private int createAheadMinutes;

    public MarketSchedulerService(MarketRepository marketRepository,
                                  MarketService marketService,
                                  PriceService priceService,
                                  SettlementService settlementService) {
        this.marketRepository = marketRepository;
        this.marketService = marketService;
        this.priceService = priceService;
        this.settlementService = settlementService;
    }

    @Scheduled(fixedDelay = 30_000)
    public void createUpcomingMarkets() {
        List<MarketTemplate> templates = marketService.activeTemplates();
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        for (MarketTemplate tpl : templates) {
            int dur = tpl.getDurationSeconds();
            int durMin = dur / 60;
            // align to duration boundary from now, create markets within the lookahead window
            LocalDateTime base = now.minusMinutes(now.getMinute() % durMin);
            for (int i = 0; i <= createAheadMinutes / durMin + 1; i++) {
                LocalDateTime start = base.plusMinutes((long) durMin * i);
                if (start.isBefore(now)) {
                    continue;
                }
                createIfAbsent(tpl, start);
            }
        }
    }

    @Transactional
    public void createIfAbsent(MarketTemplate tpl, LocalDateTime start) {
        if (marketRepository.findByTemplateAndStartTime(tpl.getId(), start).isPresent()) {
            return;
        }
        LocalDateTime end = start.plusSeconds(tpl.getDurationSeconds());
        Market m = new Market();
        m.setTemplateId(tpl.getId());
        m.setMarketNo(tpl.getAssetSymbol() + "-" + (tpl.getDurationSeconds() / 60) + "M-" + start.format(NO_FMT));
        m.setTitle(tpl.getAssetSymbol() + " " + (tpl.getDurationSeconds() / 60) + "分钟涨跌 "
                + start.toLocalTime() + "-" + end.toLocalTime());
        m.setAssetSymbol(tpl.getAssetSymbol());
        m.setStartTime(start);
        m.setEndTime(end);
        m.setResult(MarketResult.PENDING);
        m.setStatus(MarketStatus.PENDING);
        marketRepository.save(m);
        log.info("创建市场 {} {}", m.getMarketNo(), m.getTitle());
    }

    @Scheduled(fixedDelay = 2_000)
    @Transactional
    public void openMarkets() {
        LocalDateTime now = LocalDateTime.now();
        List<Market> toOpen = marketRepository.findPendingToOpen(MarketStatus.PENDING, now);
        for (Market m : toOpen) {
            BigDecimal price = priceService.getLatestPrice();
            m.setOpenPrice(price);
            m.setStatus(MarketStatus.ACTIVE);
            marketRepository.save(m);
            log.info("市场 {} 开盘 价={}", m.getMarketNo(), price);
        }
    }

    @Scheduled(fixedDelay = 2_000)
    @Transactional
    public void closeAndSettleMarkets() {
        LocalDateTime now = LocalDateTime.now();
        List<Market> toClose = marketRepository.findActiveToClose(MarketStatus.ACTIVE, now);
        for (Market m : toClose) {
            BigDecimal price = priceService.getLatestPrice();
            m.setClosePrice(price);
            marketRepository.save(m);
            log.info("市场 {} 收盘 价={}", m.getMarketNo(), price);
            try {
                settlementService.settle(m);
            } catch (Exception e) {
                log.error("市场 {} 结算异常", m.getId(), e);
            }
        }
    }
}
