package com.lazybeartoby.btcmarket.service;

import com.lazybeartoby.btcmarket.common.constant.MarketStatus;
import com.lazybeartoby.btcmarket.common.exception.BizException;
import com.lazybeartoby.btcmarket.model.entity.Market;
import com.lazybeartoby.btcmarket.model.entity.MarketTemplate;
import com.lazybeartoby.btcmarket.model.vo.MarketVO;
import com.lazybeartoby.btcmarket.repository.MarketRepository;
import com.lazybeartoby.btcmarket.repository.MarketTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarketService {

    private final MarketRepository marketRepository;
    private final MarketTemplateRepository templateRepository;

    @Value("${app.market.platform-fee-rate:0.02}")
    private BigDecimal feeRate;

    public MarketService(MarketRepository marketRepository, MarketTemplateRepository templateRepository) {
        this.marketRepository = marketRepository;
        this.templateRepository = templateRepository;
    }

    public List<MarketTemplate> activeTemplates() {
        return templateRepository.findByStatus(1);
    }

    public List<MarketVO> activeMarkets() {
        List<Market> markets = marketRepository.findByStatusInOrderByStartTimeAsc(
                List.of(MarketStatus.PENDING, MarketStatus.ACTIVE));
        return markets.stream().map(m -> toVO(m, true)).toList();
    }

    public List<MarketVO> settledHistory() {
        return marketRepository.findTop10ByStatusOrderBySettledAtDesc(MarketStatus.SETTLED)
                .stream().map(m -> toVO(m, false)).toList();
    }

    public MarketVO detail(Long id) {
        Market m = marketRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "市场不存在"));
        return toVO(m, true);
    }

    public Market requireActive(Long marketId) {
        Market m = marketRepository.findById(marketId)
                .orElseThrow(() -> new BizException(404, "市场不存在"));
        if (m.getStatus() != MarketStatus.ACTIVE) {
            throw new BizException("市场当前不可押注");
        }
        return m;
    }

    public Page<MarketVO> list(int page, int size) {
        List<MarketStatus> statuses = List.of(
                MarketStatus.PENDING, MarketStatus.ACTIVE, MarketStatus.CLOSED, MarketStatus.SETTLED);
        return marketRepository.findByStatusInOrderByStartTimeDesc(statuses, PageRequest.of(page, size))
                .map(m -> toVO(m, false));
    }

    public BigDecimal oddsUp(Market m) {
        return computeOdds(m.getTotalBetUp(), m.getTotalBetDown());
    }

    public BigDecimal oddsDown(Market m) {
        return computeOdds(m.getTotalBetDown(), m.getTotalBetUp());
    }

    private BigDecimal computeOdds(BigDecimal sameSide, BigDecimal oppositeSide) {
        if (sameSide.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("10.0000");
        }
        if (oppositeSide.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal ratio = oppositeSide.divide(sameSide, 8, RoundingMode.HALF_UP);
        BigDecimal odds = BigDecimal.ONE.add(ratio.multiply(BigDecimal.ONE.subtract(feeRate)));
        return odds.setScale(4, RoundingMode.HALF_UP);
    }

    public MarketVO toVO(Market m, boolean withOdds) {
        MarketVO vo = new MarketVO();
        vo.setId(m.getId());
        vo.setMarketNo(m.getMarketNo());
        vo.setTitle(m.getTitle());
        vo.setAssetSymbol(m.getAssetSymbol());
        vo.setStartTime(m.getStartTime());
        vo.setEndTime(m.getEndTime());
        vo.setOpenPrice(m.getOpenPrice());
        vo.setClosePrice(m.getClosePrice());
        vo.setResult(m.getResult());
        vo.setTotalBetUp(m.getTotalBetUp());
        vo.setTotalBetDown(m.getTotalBetDown());
        vo.setBetCount(m.getBetCount());
        vo.setStatus(m.getStatus());
        if (withOdds) {
            vo.setOddsUp(oddsUp(m));
            vo.setOddsDown(oddsDown(m));
            vo.setCountdownSeconds(countdown(m));
        }
        return vo;
    }

    private Long countdown(Market m) {
        LocalDateTime now = LocalDateTime.now();
        if (m.getStatus() == MarketStatus.PENDING) {
            long s = Duration.between(now, m.getStartTime()).getSeconds();
            return Math.max(0, s);
        }
        if (m.getStatus() == MarketStatus.ACTIVE) {
            long s = Duration.between(now, m.getEndTime()).getSeconds();
            return Math.max(0, s);
        }
        return 0L;
    }
}
