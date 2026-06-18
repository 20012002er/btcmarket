package com.lazybeartoby.btcmarket.controller;

import com.lazybeartoby.btcmarket.common.result.R;
import com.lazybeartoby.btcmarket.model.dto.LoginRequest;
import com.lazybeartoby.btcmarket.model.dto.RegisterRequest;
import com.lazybeartoby.btcmarket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public R<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return R.ok(authService.register(req));
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }
}
