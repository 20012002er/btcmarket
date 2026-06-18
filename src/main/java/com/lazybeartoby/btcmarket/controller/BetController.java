package com.lazybeartoby.btcmarket.controller;

import com.lazybeartoby.btcmarket.common.result.R;
import com.lazybeartoby.btcmarket.common.security.AuthChecker;
import com.lazybeartoby.btcmarket.model.dto.BetRequest;
import com.lazybeartoby.btcmarket.model.vo.BetVO;
import com.lazybeartoby.btcmarket.service.BetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bet")
public class BetController {

    private final BetService betService;
    private final AuthChecker authChecker;

    public BetController(BetService betService, AuthChecker authChecker) {
        this.betService = betService;
        this.authChecker = authChecker;
    }

    @PostMapping("/place")
    public R<BetVO> place(@Valid @RequestBody BetRequest req) {
        Long userId = authChecker.requireLogin().userId();
        return R.ok(betService.placeBet(userId, req.getMarketId(), req.getDirection(), req.getAmount()));
    }

    @GetMapping("/my")
    public R<?> my(@RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "20") int size) {
        Long userId = authChecker.requireLogin().userId();
        return R.ok(betService.myBets(userId, page, size));
    }

    @GetMapping("/market/{marketId}")
    public R<?> marketBets(@PathVariable Long marketId) {
        return R.ok(betService.marketBets(marketId));
    }
}
