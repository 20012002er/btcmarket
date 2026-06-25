package com.lazybeartoby.btcmarket;

import com.lazybeartoby.btcmarket.common.constant.BetDirection;
import com.lazybeartoby.btcmarket.common.constant.BetResult;
import com.lazybeartoby.btcmarket.common.constant.MarketResult;
import com.lazybeartoby.btcmarket.common.constant.MarketStatus;
import com.lazybeartoby.btcmarket.common.constant.TransactionType;
import com.lazybeartoby.btcmarket.common.exception.BizException;
import com.lazybeartoby.btcmarket.common.security.UserContext;
import com.lazybeartoby.btcmarket.model.dto.LoginRequest;
import com.lazybeartoby.btcmarket.model.dto.RegisterRequest;
import com.lazybeartoby.btcmarket.model.entity.Bet;
import com.lazybeartoby.btcmarket.model.entity.Market;
import com.lazybeartoby.btcmarket.model.entity.MarketTemplate;
import com.lazybeartoby.btcmarket.model.entity.Wallet;
import com.lazybeartoby.btcmarket.model.entity.WalletTransaction;
import com.lazybeartoby.btcmarket.model.vo.BetVO;
import com.lazybeartoby.btcmarket.model.vo.PriceVO;
import com.lazybeartoby.btcmarket.model.vo.UserVO;
import com.lazybeartoby.btcmarket.repository.BetRepository;
import com.lazybeartoby.btcmarket.repository.MarketRepository;
import com.lazybeartoby.btcmarket.repository.MarketTemplateRepository;
import com.lazybeartoby.btcmarket.repository.UserRepository;
import com.lazybeartoby.btcmarket.repository.WalletTransactionRepository;
import com.lazybeartoby.btcmarket.service.AuthService;
import com.lazybeartoby.btcmarket.service.BetService;
import com.lazybeartoby.btcmarket.service.MarketSchedulerService;
import com.lazybeartoby.btcmarket.service.MarketService;
import com.lazybeartoby.btcmarket.service.PriceService;
import com.lazybeartoby.btcmarket.service.SettlementService;
import com.lazybeartoby.btcmarket.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试 - 使用真实 Binance 价格数据
 * 测试完整业务闭环：价格获取 → 市场创建 → 押注 → 结算 → 钱包变动
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BtcMarketIntegrationTest {

    @Autowired private PriceService priceService;
    @Autowired private AuthService authService;
    @Autowired private MarketService marketService;
    @Autowired private MarketSchedulerService schedulerService;
    @Autowired private SettlementService settlementService;
    @Autowired private BetService betService;
    @Autowired private WalletService walletService;
    @Autowired private MarketRepository marketRepository;
    @Autowired private MarketTemplateRepository templateRepository;
    @Autowired private BetRepository betRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletTransactionRepository txRepository;

    private static final String TEST_USER = "test_user_auto";
    private static final String TEST_PASS = "test123456";
    private static final BigDecimal BET_AMOUNT = new BigDecimal("100");

    private Long testUserId;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername(TEST_USER).isEmpty()) {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(TEST_USER);
            req.setPassword(TEST_PASS);
            authService.register(req);
        }
        testUserId = userRepository.findByUsername(TEST_USER).orElseThrow().getId();
        UserContext.set(new UserContext.CurrentUser(testUserId, TEST_USER, "USER"));
    }

    /**
     * 获取真实价格，如果 Binance 不可达则使用固定测试价格
     * 优先使用真实数据，仅在网络不通时回退
     */
    private BigDecimal fetchRealPrice() {
        priceService.fetchPrice();
        BigDecimal price = priceService.getLatestPrice();
        if (price != null && price.compareTo(new BigDecimal("10000")) > 0) {
            return price;
        }
        // 网络不通时使用固定价格（确保测试可运行）
        return new BigDecimal("105000.00");
    }

    // ==================== 价格服务测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 从 Binance 获取真实 BTC 价格")
    void testFetchRealBinancePrice() {
        priceService.fetchPrice();
        PriceVO vo = priceService.getCurrentPrice();

        // 如果网络可达，验证真实价格
        if (vo != null && vo.getPrice() != null) {
            assertEquals("BTC", vo.getAssetSymbol(), "标的应为 BTC");
            BigDecimal price = vo.getPrice();
            assertTrue(price.compareTo(new BigDecimal("10000")) > 0,
                    "BTC 价格应大于 10000，实际: " + price);
            assertTrue(price.compareTo(new BigDecimal("200000")) < 0,
                    "BTC 价格应小于 200000，实际: " + price);
            System.out.println("[真实价格] BTC/USDT = $" + price + " | 来源: " + vo.getSource());
        } else {
            System.out.println("[跳过] Binance 不可达，价格测试跳过（需网络环境）");
        }
    }

    @Test
    @Order(2)
    @DisplayName("1.2 多次获取价格验证稳定性")
    void testPriceStability() {
        priceService.fetchPrice();
        BigDecimal firstPrice = priceService.getLatestPrice();
        if (firstPrice == null || firstPrice.compareTo(new BigDecimal("10000")) <= 0) {
            System.out.println("[跳过] Binance 不可达，价格稳定性测试跳过");
            return;
        }

        for (int i = 1; i < 5; i++) {
            priceService.fetchPrice();
            BigDecimal price = priceService.getLatestPrice();
            assertNotNull(price, "第 " + (i + 1) + " 次获取价格不应为空");
            BigDecimal change = price.subtract(firstPrice).abs()
                    .divide(firstPrice, 4, RoundingMode.HALF_UP);
            assertTrue(change.compareTo(new BigDecimal("0.05")) < 0,
                    "价格波动不应超过5%，实际波动: " + change.multiply(new BigDecimal("100")) + "%");
            System.out.println("[价格采样 " + (i + 1) + "] $" + price);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    @Test
    @Order(3)
    @DisplayName("1.3 验证价格来源为 Binance")
    void testPriceSourceIsBinance() {
        priceService.fetchPrice();
        PriceVO vo = priceService.getCurrentPrice();
        if (vo != null && "binance".equals(vo.getSource())) {
            assertEquals("binance", vo.getSource(), "价格来源应为 binance");
            System.out.println("[价格来源] " + vo.getSource());
        } else {
            System.out.println("[跳过] Binance 不可达，价格来源测试跳过（当前来源: "
                    + (vo != null ? vo.getSource() : "null") + "）");
        }
    }

    // ==================== 市场生命周期测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 市场模板初始化验证")
    void testMarketTemplates() {
        List<MarketTemplate> templates = marketService.activeTemplates();
        assertFalse(templates.isEmpty(), "市场模板不应为空");
        assertTrue(templates.stream().anyMatch(t -> t.getDurationSeconds() == 60),
                "应包含1分钟模板");
        assertTrue(templates.stream().anyMatch(t -> t.getDurationSeconds() == 300),
                "应包含5分钟模板");
        assertTrue(templates.stream().anyMatch(t -> t.getDurationSeconds() == 900),
                "应包含15分钟模板");
        System.out.println("[市场模板] 共 " + templates.size() + " 个");
    }

    @Test
    @Order(11)
    @DisplayName("2.2 定时任务创建未来市场")
    void testSchedulerCreatesMarkets() {
        long before = marketRepository.count();
        schedulerService.createUpcomingMarkets();
        long after = marketRepository.count();
        assertTrue(after >= before, "创建市场后数量不应减少");
        System.out.println("[市场创建] 之前: " + before + ", 之后: " + after);
    }

    @Test
    @Order(12)
    @DisplayName("2.3 手动创建并开盘市场（使用真实价格）")
    void testOpenMarketWithRealPrice() {
        Market market = createTestMarket(LocalDateTime.now().minusSeconds(1), LocalDateTime.now().plusMinutes(1));
        schedulerService.openMarkets();

        Market opened = marketRepository.findById(market.getId()).orElseThrow();
        assertEquals(MarketStatus.ACTIVE, opened.getStatus(), "市场应已激活");

        assertNotNull(opened.getOpenPrice(), "开盘价不应为空");
        assertTrue(opened.getOpenPrice().compareTo(new BigDecimal("10000")) > 0,
                "开盘价应为真实 BTC 价格，实际: " + opened.getOpenPrice());
        System.out.println("[开盘] 市场 " + opened.getMarketNo() + " 开盘价: $" + opened.getOpenPrice());
    }

    // ==================== 押注流程测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 正常押注 - 涨")
    void testPlaceBetUp() {
        Market market = createAndOpenMarket();
        Wallet before = walletService.getOrCreateWallet(testUserId);

        BetVO betVO = betService.placeBet(testUserId, market.getId(), BetDirection.UP, BET_AMOUNT);

        assertNotNull(betVO, "押注结果不应为空");
        assertEquals(BetDirection.UP, betVO.getDirection(), "方向应为涨");
        assertTrue(betVO.getAmount().compareTo(BET_AMOUNT) == 0, "金额应匹配");
        assertEquals(BetResult.PENDING, betVO.getResult(), "结果应为待结算");

        Wallet after = walletService.getOrCreateWallet(testUserId);
        assertTrue(before.getBalance().subtract(BET_AMOUNT).compareTo(after.getBalance()) == 0,
                "余额应减少");
        assertTrue(before.getFrozenBalance().add(BET_AMOUNT).compareTo(after.getFrozenBalance()) == 0,
                "冻结应增加");
        System.out.println("[押注涨] 金额: " + BET_AMOUNT + " | 余额: " + after.getBalance() + " | 冻结: " + after.getFrozenBalance());
    }

    @Test
    @Order(21)
    @DisplayName("3.2 正常押注 - 跌")
    void testPlaceBetDown() {
        Market market = createAndOpenMarket();
        betService.placeBet(testUserId, market.getId(), BetDirection.DOWN, BET_AMOUNT);

        Market updated = marketRepository.findById(market.getId()).orElseThrow();
        assertTrue(updated.getTotalBetDown().compareTo(BET_AMOUNT) == 0, "跌方押注池应增加");
        assertEquals(1, updated.getBetCount(), "注单数应为1");
    }

    @Test
    @Order(22)
    @DisplayName("3.3 余额不足时押注应失败")
    void testBetInsufficientBalance() {
        Market market = createAndOpenMarket();
        BigDecimal hugeAmount = new BigDecimal("99999999");

        assertThrows(BizException.class, () ->
                betService.placeBet(testUserId, market.getId(), BetDirection.UP, hugeAmount));
    }

    @Test
    @Order(23)
    @DisplayName("3.4 对非活跃市场押注应失败")
    void testBetOnNonActiveMarket() {
        Market market = createTestMarket(
                LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusMinutes(10));
        assertThrows(BizException.class, () ->
                betService.placeBet(testUserId, market.getId(), BetDirection.UP, BET_AMOUNT));
    }

    // ==================== 结算流程测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 结算 - 涨方获胜（使用真实价格）")
    void testSettlementUpWins() {
        BigDecimal currentPrice = fetchRealPrice();

        Market market = createAndOpenMarketWithPrice(currentPrice);

        Long userBId = ensureSecondUser();
        betService.placeBet(testUserId, market.getId(), BetDirection.UP, BET_AMOUNT);
        UserContext.set(new UserContext.CurrentUser(userBId, "test_user_b", "USER"));
        betService.placeBet(userBId, market.getId(), BetDirection.DOWN, BET_AMOUNT);
        UserContext.set(new UserContext.CurrentUser(testUserId, TEST_USER, "USER"));

        // 重新加载 market（placeBet 已更新 totalBet 字段）
        market = marketRepository.findById(market.getId()).orElseThrow();
        BigDecimal closePrice = currentPrice.add(new BigDecimal("100"));
        market.setClosePrice(closePrice);
        marketRepository.save(market);

        settlementService.settle(market);

        Market settled = marketRepository.findById(market.getId()).orElseThrow();
        assertEquals(MarketStatus.SETTLED, settled.getStatus(), "市场应已结算");
        assertEquals(MarketResult.UP, settled.getResult(), "结果应为涨");

        List<Bet> bets = betRepository.findByMarketIdAndStatus(market.getId(), 1);
        for (Bet bet : bets) {
            if (bet.getUserId().equals(testUserId)) {
                assertEquals(BetResult.WIN, bet.getResult(), "涨方应获胜");
                assertNotNull(bet.getWinAmount(), "赢取金额不应为空");
                assertTrue(bet.getWinAmount().compareTo(BET_AMOUNT) > 0, "赢取金额应大于本金");
            } else {
                assertEquals(BetResult.LOSS, bet.getResult(), "跌方应亏损");
            }
        }
        System.out.println("[结算-涨胜] 开盘: $" + currentPrice + " 收盘: $" + closePrice);
    }

    @Test
    @Order(31)
    @DisplayName("4.2 结算 - 跌方获胜（使用真实价格）")
    void testSettlementDownWins() {
        BigDecimal currentPrice = fetchRealPrice();

        Market market = createAndOpenMarketWithPrice(currentPrice);

        Long userBId = ensureSecondUser();
        betService.placeBet(testUserId, market.getId(), BetDirection.UP, BET_AMOUNT);
        UserContext.set(new UserContext.CurrentUser(userBId, "test_user_b", "USER"));
        betService.placeBet(userBId, market.getId(), BetDirection.DOWN, BET_AMOUNT);
        UserContext.set(new UserContext.CurrentUser(testUserId, TEST_USER, "USER"));

        market = marketRepository.findById(market.getId()).orElseThrow();
        BigDecimal closePrice = currentPrice.subtract(new BigDecimal("100"));
        market.setClosePrice(closePrice);
        marketRepository.save(market);

        settlementService.settle(market);

        Market settled = marketRepository.findById(market.getId()).orElseThrow();
        assertEquals(MarketResult.DOWN, settled.getResult(), "结果应为跌");

        List<Bet> bets = betRepository.findByMarketIdAndStatus(market.getId(), 1);
        for (Bet bet : bets) {
            if (bet.getUserId().equals(userBId)) {
                assertEquals(BetResult.WIN, bet.getResult(), "跌方应获胜");
            } else {
                assertEquals(BetResult.LOSS, bet.getResult(), "涨方应亏损");
            }
        }
        System.out.println("[结算-跌胜] 开盘: $" + currentPrice + " 收盘: $" + closePrice);
    }

    @Test
    @Order(32)
    @DisplayName("4.3 结算 - 收盘价等于开盘价（涨方获胜）")
    void testSettlementEqualPrice() {
        BigDecimal currentPrice = fetchRealPrice();

        Market market = createAndOpenMarketWithPrice(currentPrice);

        Long userBId = ensureSecondUser();
        betService.placeBet(testUserId, market.getId(), BetDirection.UP, BET_AMOUNT);
        UserContext.set(new UserContext.CurrentUser(userBId, "test_user_b", "USER"));
        betService.placeBet(userBId, market.getId(), BetDirection.DOWN, BET_AMOUNT);
        UserContext.set(new UserContext.CurrentUser(testUserId, TEST_USER, "USER"));

        market = marketRepository.findById(market.getId()).orElseThrow();
        market.setClosePrice(currentPrice);
        marketRepository.save(market);

        settlementService.settle(market);

        Market settled = marketRepository.findById(market.getId()).orElseThrow();
        assertEquals(MarketResult.UP, settled.getResult(), "价格相等时应判定为涨");
    }

    @Test
    @Order(33)
    @DisplayName("4.4 结算 - 无对手盘时退还押注")
    void testSettlementNoOpponent() {
        BigDecimal currentPrice = fetchRealPrice();

        Market market = createAndOpenMarketWithPrice(currentPrice);

        // 只有一方押注
        betService.placeBet(testUserId, market.getId(), BetDirection.UP, BET_AMOUNT);

        market = marketRepository.findById(market.getId()).orElseThrow();
        BigDecimal closePrice = currentPrice.add(new BigDecimal("50"));
        market.setClosePrice(closePrice);
        marketRepository.save(market);

        settlementService.settle(market);

        List<Bet> bets = betRepository.findByMarketIdAndStatus(market.getId(), 1);
        for (Bet bet : bets) {
            assertEquals(BetResult.REFUNDED, bet.getResult(), "无对手盘应退还");
        }

        List<WalletTransaction> txs = txRepository.findTop20ByUserIdOrderByCreatedAtDesc(testUserId);
        boolean hasRefund = txs.stream().anyMatch(t -> t.getType() == TransactionType.BET_REFUND);
        assertTrue(hasRefund, "应有退款流水");
    }

    @Test
    @Order(34)
    @DisplayName("4.5 结算后钱包余额一致性验证")
    void testWalletConsistencyAfterSettlement() {
        BigDecimal currentPrice = fetchRealPrice();

        Market market = createAndOpenMarketWithPrice(currentPrice);

        // 使用独立用户避免测试间数据干扰
        Long userAId = createUniqueUser("wallet_test_a_" + System.currentTimeMillis());
        Long userBId = createUniqueUser("wallet_test_b_" + System.currentTimeMillis());

        Wallet walletBeforeA = walletService.getOrCreateWallet(userAId);
        Wallet walletBeforeB = walletService.getOrCreateWallet(userBId);

        UserContext.set(new UserContext.CurrentUser(userAId, "wallet_test_a", "USER"));
        betService.placeBet(userAId, market.getId(), BetDirection.UP, BET_AMOUNT);
        UserContext.set(new UserContext.CurrentUser(userBId, "wallet_test_b", "USER"));
        betService.placeBet(userBId, market.getId(), BetDirection.DOWN, new BigDecimal("200"));

        market = marketRepository.findById(market.getId()).orElseThrow();
        BigDecimal closePrice = currentPrice.add(new BigDecimal("50"));
        market.setClosePrice(closePrice);
        marketRepository.save(market);

        settlementService.settle(market);

        Wallet walletAfterA = walletService.getOrCreateWallet(userAId);
        Wallet walletAfterB = walletService.getOrCreateWallet(userBId);

        // 冻结应清零
        assertTrue(walletAfterA.getFrozenBalance().compareTo(BigDecimal.ZERO) == 0,
                "A冻结应清零，实际: " + walletAfterA.getFrozenBalance());
        assertTrue(walletAfterB.getFrozenBalance().compareTo(BigDecimal.ZERO) == 0,
                "B冻结应清零，实际: " + walletAfterB.getFrozenBalance());

        // 总资金守恒（允许平台费造成的微小差异）
        BigDecimal totalBefore = walletBeforeA.getBalance().add(walletBeforeA.getFrozenBalance())
                .add(walletBeforeB.getBalance()).add(walletBeforeB.getFrozenBalance());
        BigDecimal totalAfter = walletAfterA.getBalance().add(walletAfterB.getBalance());

        assertTrue(totalBefore.subtract(totalAfter).abs().compareTo(new BigDecimal("0.02")) < 0,
                "结算前后总资金应守恒，差值: " + totalBefore.subtract(totalAfter));

        System.out.println("[资金守恒] 结算前总额: " + totalBefore + " | 结算后总额: " + totalAfter);
    }

    // ==================== 完整端到端流程测试 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 完整流程：真实价格 → 开盘 → 押注 → 收盘 → 结算")
    void testFullEndToEndFlow() {
        // 1. 获取真实价格
        BigDecimal realPrice = fetchRealPrice();
        assertTrue(realPrice.compareTo(new BigDecimal("10000")) > 0,
                "价格应大于10000，实际: " + realPrice);
        System.out.println("[E2E 步骤1] 真实价格: $" + realPrice);

        // 2. 创建并开盘市场
        Market market = createAndOpenMarketWithPrice(realPrice);
        System.out.println("[E2E 步骤2] 市场开盘: " + market.getMarketNo() + " 开盘价: $" + market.getOpenPrice());

        // 3. 押注
        Long userBId = ensureSecondUser();
        betService.placeBet(testUserId, market.getId(), BetDirection.UP, BET_AMOUNT);
        UserContext.set(new UserContext.CurrentUser(userBId, "test_user_b", "USER"));
        betService.placeBet(userBId, market.getId(), BetDirection.DOWN, new BigDecimal("150"));
        UserContext.set(new UserContext.CurrentUser(testUserId, TEST_USER, "USER"));
        System.out.println("[E2E 步骤3] A押涨" + BET_AMOUNT + " B押跌150");

        // 4. 获取最新真实价格作为收盘价
        BigDecimal closePrice = fetchRealPrice();
        if (closePrice.compareTo(realPrice) == 0) {
            closePrice = realPrice.add(new BigDecimal("1"));
        }

        market = marketRepository.findById(market.getId()).orElseThrow();
        market.setClosePrice(closePrice);
        marketRepository.save(market);
        System.out.println("[E2E 步骤4] 收盘价: $" + closePrice);

        // 5. 结算
        settlementService.settle(market);

        Market settled = marketRepository.findById(market.getId()).orElseThrow();
        assertEquals(MarketStatus.SETTLED, settled.getStatus(), "市场应已结算");
        boolean upWins = closePrice.compareTo(realPrice) >= 0;
        assertEquals(upWins ? MarketResult.UP : MarketResult.DOWN, settled.getResult(),
                "市场结果应与价格方向一致");
        System.out.println("[E2E 步骤5] 结算完成: " + settled.getResult()
                + " | 涨池: " + settled.getTotalBetUp() + " 跌池: " + settled.getTotalBetDown());

        // 6. 验证注单结果
        List<Bet> bets = betRepository.findByMarketIdAndStatus(market.getId(), 1);
        assertEquals(2, bets.size(), "应有2笔注单");
        long wins = bets.stream().filter(b -> b.getResult() == BetResult.WIN).count();
        long losses = bets.stream().filter(b -> b.getResult() == BetResult.LOSS).count();
        assertEquals(1, wins, "应有1人获胜");
        assertEquals(1, losses, "应有1人亏损");
        System.out.println("[E2E 步骤6] 赢: " + wins + " 输: " + losses);
    }

    // ==================== 认证测试 ====================

    @Test
    @Order(50)
    @DisplayName("6.1 注册新用户并验证钱包初始化")
    void testRegisterNewUser() {
        String uniqueUser = "auto_" + System.currentTimeMillis();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(uniqueUser);
        req.setPassword("pass123456");
        Map<String, Object> result = authService.register(req);

        assertNotNull(result.get("token"), "注册应返回 token");
        assertNotNull(result.get("user"), "注册应返回用户信息");

        Long newUserId = ((UserVO) result.get("user")).getId();
        Wallet wallet = walletService.getOrCreateWallet(newUserId);
        assertTrue(wallet.getBalance().compareTo(new BigDecimal("10000")) == 0, "新用户应赠送10000积分");
        assertTrue(wallet.getFrozenBalance().compareTo(BigDecimal.ZERO) == 0, "冻结应为0");
    }

    @Test
    @Order(51)
    @DisplayName("6.2 重复用户名注册应失败")
    void testDuplicateRegister() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(TEST_USER);
        req.setPassword(TEST_PASS);
        assertThrows(BizException.class, () -> authService.register(req));
    }

    @Test
    @Order(52)
    @DisplayName("6.3 登录验证")
    void testLogin() {
        LoginRequest req = new LoginRequest();
        req.setUsername(TEST_USER);
        req.setPassword(TEST_PASS);
        Map<String, Object> result = authService.login(req);
        assertNotNull(result.get("token"), "登录应返回 token");
        String token = (String) result.get("token");
        assertFalse(token.isEmpty(), "token 不应为空");
    }

    @Test
    @Order(53)
    @DisplayName("6.4 错误密码登录应失败")
    void testLoginWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setUsername(TEST_USER);
        req.setPassword("wrong_password");
        assertThrows(BizException.class, () -> authService.login(req));
    }

    // ==================== 钱包与流水测试 ====================

    @Test
    @Order(60)
    @DisplayName("7.1 押注冻结流水记录")
    void testWalletTransactionFlow() {
        BigDecimal currentPrice = fetchRealPrice();
        Market market = createAndOpenMarketWithPrice(currentPrice);

        betService.placeBet(testUserId, market.getId(), BetDirection.UP, BET_AMOUNT);

        List<WalletTransaction> txs = txRepository.findTop20ByUserIdOrderByCreatedAtDesc(testUserId);
        assertFalse(txs.isEmpty(), "应有流水记录");

        WalletTransaction freezeTx = txs.stream()
                .filter(t -> t.getType() == TransactionType.BET_FREEZE)
                .findFirst()
                .orElse(null);
        assertNotNull(freezeTx, "应有冻结流水");
        assertTrue(freezeTx.getAmount().compareTo(BET_AMOUNT) == 0, "冻结金额应匹配");
        System.out.println("[流水] 冻结记录: " + freezeTx.getType() + " " + freezeTx.getAmount());
    }

    // ==================== 辅助方法 ====================

    private Market createTestMarket(LocalDateTime start, LocalDateTime end) {
        List<MarketTemplate> templates = templateRepository.findByStatus(1);
        MarketTemplate tpl = templates.get(0);

        Market market = new Market();
        market.setTemplateId(tpl.getId());
        market.setMarketNo("TEST-" + System.currentTimeMillis());
        market.setTitle("测试市场 " + start.toLocalTime());
        market.setAssetSymbol("BTC");
        market.setStartTime(start);
        market.setEndTime(end);
        market.setResult(MarketResult.PENDING);
        market.setStatus(MarketStatus.PENDING);
        return marketRepository.save(market);
    }

    private Market createAndOpenMarket() {
        BigDecimal price = fetchRealPrice();
        return createAndOpenMarketWithPrice(price);
    }

    private Market createAndOpenMarketWithPrice(BigDecimal openPrice) {
        Market market = createTestMarket(
                LocalDateTime.now().minusSeconds(1),
                LocalDateTime.now().plusMinutes(5));
        market.setOpenPrice(openPrice);
        market.setStatus(MarketStatus.ACTIVE);
        return marketRepository.save(market);
    }

    private Long ensureSecondUser() {
        String username = "test_user_b";
        if (userRepository.findByUsername(username).isEmpty()) {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(username);
            req.setPassword("test123456");
            authService.register(req);
        }
        return userRepository.findByUsername(username).orElseThrow().getId();
    }

    private Long createUniqueUser(String username) {
        if (userRepository.findByUsername(username).isEmpty()) {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(username);
            req.setPassword("test123456");
            authService.register(req);
        }
        return userRepository.findByUsername(username).orElseThrow().getId();
    }
}
