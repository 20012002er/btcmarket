package com.lazybeartoby.btcmarket.service;

import com.lazybeartoby.btcmarket.common.constant.MarketStatus;
import com.lazybeartoby.btcmarket.common.exception.BizException;
import com.lazybeartoby.btcmarket.model.entity.User;
import com.lazybeartoby.btcmarket.model.entity.Wallet;
import com.lazybeartoby.btcmarket.model.vo.UserVO;
import com.lazybeartoby.btcmarket.repository.MarketRepository;
import com.lazybeartoby.btcmarket.repository.UserRepository;
import com.lazybeartoby.btcmarket.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final MarketRepository marketRepository;
    private final AuthService authService;

    public AdminService(UserRepository userRepository,
                        WalletRepository walletRepository,
                        WalletService walletService,
                        MarketRepository marketRepository,
                        AuthService authService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.marketRepository = marketRepository;
        this.authService = authService;
    }

    @Transactional
    public Map<String, Object> grantPoints(Long adminId, Long targetUserId, BigDecimal amount, String remark, boolean deduct) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BizException(404, "目标用户不存在"));
        walletService.adminGrant(targetUserId, amount, remark, adminId, deduct);
        Wallet w = walletService.getOrCreateWallet(targetUserId);
        Map<String, Object> res = new HashMap<>();
        res.put("userId", targetUserId);
        res.put("username", target.getUsername());
        res.put("balance", w.getBalance());
        res.put("frozenBalance", w.getFrozenBalance());
        return res;
    }

    public List<UserVO> users() {
        return userRepository.findAll().stream().map(authService::toVO).toList();
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> dash = new HashMap<>();
        dash.put("totalUsers", userRepository.count());
        dash.put("totalBalance", walletRepository.totalBalance());
        dash.put("activeUsers", walletRepository.countActive(BigDecimal.ZERO));
        dash.put("activeMarkets", marketRepository.findByStatusInOrderByStartTimeAsc(
                List.of(MarketStatus.PENDING, MarketStatus.ACTIVE)).size());
        dash.put("settledMarkets", marketRepository.findByStatus(MarketStatus.SETTLED).size());
        return dash;
    }
}
