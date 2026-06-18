package com.lazybeartoby.btcmarket.controller;

import com.lazybeartoby.btcmarket.common.result.R;
import com.lazybeartoby.btcmarket.model.vo.PriceVO;
import com.lazybeartoby.btcmarket.service.PriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
