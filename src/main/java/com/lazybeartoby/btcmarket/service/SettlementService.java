package com.lazybeartoby.btcmarket.service;

import com.lazybeartoby.btcmarket.common.constant.BetDirection;
import com.lazybeartoby.btcmarket.common.constant.BetResult;
import com.lazybeartoby.btcmarket.common.constant.MarketResult;
import com.lazybeartoby.btcmarket.common.constant.MarketStatus;
import com.lazybeartoby.btcmarket.model.entity.Bet;
import com.lazybeartoby.btcmarket.model.entity.Market;
import com.lazybeartoby.btcmarket.repository.BetRepository;
import com.lazybeartoby.btcmarket.repository.MarketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final MarketRepository marketRepository;
    private final BetRepository betRepository;
    private final WalletService walletService;

    public SettlementService(MarketRepository marketRepository,
                             BetRepository betRepository,
                             WalletService walletService) {
        this.marketRepository = marketRepository;
        this.betRepository = betRepository;
        this.walletService = walletService;
    }

    @Transactional
    public void settle(Market market) {
        if (market.getStatus() == MarketStatus.SETTLED) {
            return;
        }
        BigDecimal open = market.getOpenPrice();
        BigDecimal close = market.getClosePrice();
        if (open == null || close == null) {
            log.warn("市场 {} 缺少开盘/收盘价，无法结算", market.getId());
            return;
        }

        MarketResult result = close.compareTo(open) >= 0 ? MarketResult.UP : MarketResult.DOWN;
        market.setResult(result);
        market.setStatus(MarketStatus.CLOSED);

        BigDecimal winPool = result == MarketResult.UP ? market.getTotalBetUp() : market.getTotalBetDown();
        BigDecimal losePool = result == MarketResult.UP ? market.getTotalBetDown() : market.getTotalBetUp();

        List<Bet> bets = betRepository.findByMarketIdAndStatus(market.getId(), 1);

        if (winPool.compareTo(BigDecimal.ZERO) == 0 || losePool.compareTo(BigDecimal.ZERO) == 0) {
            // no winners or no losers -> refund all
            for (Bet bet : bets) {
                walletService.refundBet(bet.getUserId(), bet.getAmount(), bet.getId(), "市场结算: 无对手盘，退还押注");
                bet.setResult(BetResult.REFUNDED);
                bet.setWinAmount(bet.getAmount());
                bet.setSettledAt(LocalDateTime.now());
                betRepository.save(bet);
            }
        } else {
            for (Bet bet : bets) {
                boolean isWinner = (result == MarketResult.UP && bet.getDirection() == BetDirection.UP)
                        || (result == MarketResult.DOWN && bet.getDirection() == BetDirection.DOWN);
                if (isWinner) {
                    BigDecimal share = bet.getAmount().divide(winPool, 8, RoundingMode.HALF_UP);
                    BigDecimal profit = losePool.multiply(share).setScale(2, RoundingMode.DOWN);
                    BigDecimal returnAmount = bet.getAmount();
                    walletService.settleWin(bet.getUserId(), returnAmount, profit, bet.getId(),
                            "押" + (bet.getDirection() == BetDirection.UP ? "涨" : "跌") + "获胜 +" + profit);
                    bet.setResult(BetResult.WIN);
                    bet.setWinAmount(returnAmount.add(profit));
                } else {
                    walletService.settleLoss(bet.getUserId(), bet.getAmount(), bet.getId(),
                            "押" + (bet.getDirection() == BetDirection.UP ? "涨" : "跌") + "亏损 -" + bet.getAmount());
                    bet.setResult(BetResult.LOSS);
                    bet.setWinAmount(BigDecimal.ZERO);
                }
                bet.setSettledAt(LocalDateTime.now());
                betRepository.save(bet);
            }
        }

        market.setStatus(MarketStatus.SETTLED);
        market.setSettledAt(LocalDateTime.now());
        marketRepository.save(market);
        log.info("市场 {} 结算完成 结果={} 涨池={} 跌池={}",
                market.getId(), result, market.getTotalBetUp(), market.getTotalBetDown());
    }
}
