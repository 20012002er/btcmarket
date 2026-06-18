package com.lazybeartoby.btcmarket.controller;

import com.lazybeartoby.btcmarket.common.result.R;
import com.lazybeartoby.btcmarket.common.security.AuthChecker;
import com.lazybeartoby.btcmarket.repository.BetRepository;
import com.lazybeartoby.btcmarket.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final AuthChecker authChecker;
    private final BetRepository betRepository;

    public UserController(UserService userService, AuthChecker authChecker, BetRepository betRepository) {
        this.userService = userService;
        this.authChecker = authChecker;
        this.betRepository = betRepository;
    }

    @GetMapping("/profile")
    public R<?> profile() {
        Long userId = authChecker.requireLogin().userId();
        return R.ok(userService.profile(userId));
    }

    @GetMapping("/stats")
    public R<?> stats() {
        Long userId = authChecker.requireLogin().userId();
        Map<String, Object> stats = new HashMap<>();
        long total = betRepository.countByUserId(userId);
        long wins = betRepository.countByUserIdAndResult(userId, com.lazybeartoby.btcmarket.common.constant.BetResult.WIN);
        stats.put("totalBets", total);
        stats.put("wins", wins);
        stats.put("winRate", total == 0 ? 0 : (wins * 100 / total));
        stats.put("profit", betRepository.profitByUser(userId));
        return R.ok(stats);
    }
}
