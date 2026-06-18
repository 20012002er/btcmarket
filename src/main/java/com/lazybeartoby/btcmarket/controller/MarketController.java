package com.lazybeartoby.btcmarket.controller;

import com.lazybeartoby.btcmarket.common.result.R;
import com.lazybeartoby.btcmarket.model.vo.MarketVO;
import com.lazybeartoby.btcmarket.service.MarketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/active")
    public R<List<MarketVO>> active() {
        return R.ok(marketService.activeMarkets());
    }

    @GetMapping("/history")
    public R<List<MarketVO>> history() {
        return R.ok(marketService.settledHistory());
    }

    @GetMapping("/{id}")
    public R<MarketVO> detail(@PathVariable Long id) {
        return R.ok(marketService.detail(id));
    }

    @GetMapping("/template/list")
    public R<?> templates() {
        return R.ok(marketService.activeTemplates());
    }
}
