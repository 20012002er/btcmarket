package com.lazybeartoby.btcmarket.service;

import com.lazybeartoby.btcmarket.common.constant.BetDirection;
import com.lazybeartoby.btcmarket.common.constant.BetResult;
import com.lazybeartoby.btcmarket.common.constant.MarketStatus;
import com.lazybeartoby.btcmarket.common.exception.BizException;
import com.lazybeartoby.btcmarket.model.entity.Bet;
import com.lazybeartoby.btcmarket.model.entity.Market;
import com.lazybeartoby.btcmarket.model.vo.BetVO;
import com.lazybeartoby.btcmarket.repository.BetRepository;
import com.lazybeartoby.btcmarket.repository.MarketRepository;
import com.lazybeartoby.btcmarket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BetService {

    private final BetRepository betRepository;
    private final MarketRepository marketRepository;
    private final WalletService walletService;
    private final MarketService marketService;
    private final UserRepository userRepository;

    @Value("${app.market.min-bet:1}")
    private BigDecimal minBet;

    @Value("${app.market.max-bet:100000}")
    private BigDecimal maxBet;

    public BetService(BetRepository betRepository,
                     MarketRepository marketRepository,
                     WalletService walletService,
                     MarketService marketService,
                     UserRepository userRepository) {
        this.betRepository = betRepository;
        this.marketRepository = marketRepository;
        this.walletService = walletService;
        this.marketService = marketService;
        this.userRepository = userRepository;
    }

    @Transactional
    public BetVO placeBet(Long userId, Long marketId, BetDirection direction, BigDecimal amount) {
        if (amount.compareTo(minBet) < 0) {
            throw new BizException("最小押注 " + minBet + " 积分");
        }
        if (amount.compareTo(maxBet) > 0) {
            throw new BizException("最大押注 " + maxBet + " 积分");
        }

        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new BizException(404, "市场不存在"));
        if (market.getStatus() != MarketStatus.ACTIVE) {
            throw new BizException("市场当前不可押注");
        }

        BigDecimal odds = direction == BetDirection.UP
                ? marketService.oddsUp(market)
                : marketService.oddsDown(market);

        Bet bet = new Bet();
        bet.setBetNo(generateBetNo());
        bet.setMarketId(marketId);
        bet.setUserId(userId);
        bet.setDirection(direction);
        bet.setAmount(amount);
        bet.setOdds(odds);
        bet.setResult(BetResult.PENDING);
        bet.setStatus(1);
        bet = betRepository.save(bet);

        walletService.freezeForBet(userId, amount, bet.getId());

        if (direction == BetDirection.UP) {
            market.setTotalBetUp(market.getTotalBetUp().add(amount));
        } else {
            market.setTotalBetDown(market.getTotalBetDown().add(amount));
        }
        market.setBetCount(market.getBetCount() + 1);
        marketRepository.save(market);

        return toVO(bet, market);
    }

    public Page<BetVO> myBets(Long userId, int page, int size) {
        return betRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toVOSimple);
    }

    public List<BetVO> marketBets(Long marketId) {
        return betRepository.findByMarketIdOrderByCreatedAtDesc(marketId).stream()
                .map(this::toVOSimple)
                .toList();
    }

    private BetVO toVO(Bet bet, Market market) {
        BetVO vo = baseVO(bet);
        vo.setMarketTitle(market.getTitle());
        vo.setAssetSymbol(market.getAssetSymbol());
        return vo;
    }

    private BetVO toVOSimple(Bet bet) {
        return baseVO(bet);
    }

    private BetVO baseVO(Bet bet) {
        BetVO vo = new BetVO();
        vo.setId(bet.getId());
        vo.setBetNo(bet.getBetNo());
        vo.setMarketId(bet.getMarketId());
        vo.setDirection(bet.getDirection());
        vo.setAmount(bet.getAmount());
        vo.setOdds(bet.getOdds());
        vo.setWinAmount(bet.getWinAmount());
        vo.setResult(bet.getResult());
        vo.setCreatedAt(bet.getCreatedAt());
        vo.setSettledAt(bet.getSettledAt());
        userRepository.findById(bet.getUserId()).ifPresent(u -> vo.setUsername(u.getUsername()));
        return vo;
    }

    private String generateBetNo() {
        return "B" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    void setSettledAtNow(Bet bet) {
        bet.setSettledAt(LocalDateTime.now());
    }
}
