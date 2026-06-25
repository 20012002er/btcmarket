package com.lazybeartoby.btcmarket.controller;

import com.lazybeartoby.btcmarket.common.result.R;
import com.lazybeartoby.btcmarket.model.entity.PriceTick;
import com.lazybeartoby.btcmarket.model.vo.PriceVO;
import com.lazybeartoby.btcmarket.service.PriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/price")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/btc")
    public R<PriceVO> btc() {
        return R.ok(priceService.getCurrentPrice());
    }

    @GetMapping("/btc/history")
    public R<List<PriceTick>> btcHistory(@RequestParam(defaultValue = "60") int limit) {
        return R.ok(priceService.recentTicks(Math.min(limit, 200)));
    }
}
