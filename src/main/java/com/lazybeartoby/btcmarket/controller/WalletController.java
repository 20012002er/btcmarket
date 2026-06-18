package com.lazybeartoby.btcmarket.controller;

import com.lazybeartoby.btcmarket.common.result.R;
import com.lazybeartoby.btcmarket.common.security.AuthChecker;
import com.lazybeartoby.btcmarket.service.WalletService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final AuthChecker authChecker;

    public WalletController(WalletService walletService, AuthChecker authChecker) {
        this.walletService = walletService;
        this.authChecker = authChecker;
    }

    @GetMapping
    public R<?> myWallet() {
        Long userId = authChecker.requireLogin().userId();
        return R.ok(walletService.getWalletVO(userId));
    }

    @GetMapping("/transactions")
    public R<?> transactions(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        Long userId = authChecker.requireLogin().userId();
        return R.ok(walletService.transactions(userId, page, size));
    }
}
