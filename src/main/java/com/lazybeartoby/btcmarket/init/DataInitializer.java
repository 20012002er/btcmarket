package com.lazybeartoby.btcmarket.init;

import com.lazybeartoby.btcmarket.common.constant.AppConstants;
import com.lazybeartoby.btcmarket.model.entity.MarketTemplate;
import com.lazybeartoby.btcmarket.model.entity.User;
import com.lazybeartoby.btcmarket.model.entity.Wallet;
import com.lazybeartoby.btcmarket.repository.MarketTemplateRepository;
import com.lazybeartoby.btcmarket.repository.UserRepository;
import com.lazybeartoby.btcmarket.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MarketTemplateRepository templateRepository;

    public DataInitializer(UserRepository userRepository,
                           WalletRepository walletRepository,
                           MarketTemplateRepository templateRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.templateRepository = templateRepository;
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
