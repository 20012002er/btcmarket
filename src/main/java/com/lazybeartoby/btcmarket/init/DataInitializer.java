package com.lazybeartoby.btcmarket.init;

import com.lazybeartoby.btcmarket.common.constant.AppConstants;
import com.lazybeartoby.btcmarket.model.entity.MarketTemplate;
import com.lazybeartoby.btcmarket.model.entity.PriceTick;
import com.lazybeartoby.btcmarket.model.entity.User;
import com.lazybeartoby.btcmarket.model.entity.Wallet;
import com.lazybeartoby.btcmarket.repository.MarketTemplateRepository;
import com.lazybeartoby.btcmarket.repository.PriceTickRepository;
import com.lazybeartoby.btcmarket.repository.UserRepository;
import com.lazybeartoby.btcmarket.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MarketTemplateRepository templateRepository;
    private final PriceTickRepository priceTickRepository;

    public DataInitializer(UserRepository userRepository,
                           WalletRepository walletRepository,
                           MarketTemplateRepository templateRepository,
                           PriceTickRepository priceTickRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.templateRepository = templateRepository;
        this.priceTickRepository = priceTickRepository;
    }

    @Override
    public void run(String... args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(encoder.encode("admin123"));
            admin.setRole(AppConstants.ROLE_ADMIN);
            admin.setStatus(1);
            admin = userRepository.save(admin);

            Wallet w = new Wallet();
            w.setUserId(admin.getId());
            w.setBalance(new BigDecimal("1000000"));
            w.setFrozenBalance(BigDecimal.ZERO);
            walletRepository.save(w);
            log.info("初始化管理员账号: admin / admin123");
        }

        if (userRepository.findByUsername("demo").isEmpty()) {
            User demo = new User();
            demo.setUsername("demo");
            demo.setPasswordHash(encoder.encode("demo123"));
            demo.setRole(AppConstants.ROLE_USER);
            demo.setStatus(1);
            demo = userRepository.save(demo);

            Wallet w = new Wallet();
            w.setUserId(demo.getId());
            w.setBalance(new BigDecimal("10000"));
            w.setFrozenBalance(BigDecimal.ZERO);
            walletRepository.save(w);
            log.info("初始化演示账号: demo / demo123");
        }

        if (templateRepository.count() == 0) {
            saveTemplate("BTC 1分钟涨跌", "BTC", "比特币", 60);
            saveTemplate("BTC 5分钟涨跌", "BTC", "比特币", 300);
            saveTemplate("BTC 15分钟涨跌", "BTC", "比特币", 900);
            log.info("初始化市场模板");
        }

        // 预生成历史价格数据，确保首页折线图一打开就有数据
        if (priceTickRepository.count() == 0) {
            generateHistoricalTicks();
        }
    }

    private void generateHistoricalTicks() {
        // 生成过去2小时的价格数据，每10秒一个点 = 720条
        BigDecimal basePrice = new BigDecimal("107000.00");
        BigDecimal price = basePrice;
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        LocalDateTime startTime = now.minusHours(2);

        List<PriceTick> ticks = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 720; i++) {
            // 随机游走，模拟真实价格波动
            BigDecimal delta = price.multiply(new BigDecimal("0.0003"));
            BigDecimal move = delta.multiply(BigDecimal.valueOf(random.nextDouble(-1, 1)))
                    .setScale(2, RoundingMode.HALF_UP);
            price = price.add(move).setScale(2, RoundingMode.HALF_UP);

            // 确保价格不会偏离太远
            if (price.compareTo(basePrice.multiply(new BigDecimal("0.98"))) < 0) {
                price = basePrice.multiply(new BigDecimal("0.98"));
            }
            if (price.compareTo(basePrice.multiply(new BigDecimal("1.02"))) > 0) {
                price = basePrice.multiply(new BigDecimal("1.02"));
            }

            PriceTick tick = new PriceTick();
            tick.setAssetSymbol(AppConstants.ASSET_BTC);
            tick.setPrice(price);
            tick.setTimestamp(startTime.plusSeconds(i * 10));
            tick.setSource("init");
            ticks.add(tick);
        }

        priceTickRepository.saveAll(ticks);
        log.info("初始化历史价格数据: {} 条", ticks.size());
    }

    private void saveTemplate(String name, String symbol, String assetName, int duration) {
        MarketTemplate tpl = new MarketTemplate();
        tpl.setName(name);
        tpl.setAssetSymbol(symbol);
        tpl.setAssetName(assetName);
        tpl.setDurationSeconds(duration);
        tpl.setStatus(1);
        tpl.setSettlementRule("收盘价≥开盘价=涨方赢，否则跌方赢");
        templateRepository.save(tpl);
    }
}
