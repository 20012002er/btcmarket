package com.lazybeartoby.btcmarket.controller;

import com.lazybeartoby.btcmarket.common.result.R;
import com.lazybeartoby.btcmarket.common.security.AuthChecker;
import com.lazybeartoby.btcmarket.model.dto.GrantPointsRequest;
import com.lazybeartoby.btcmarket.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthChecker authChecker;

    public AdminController(AdminService adminService, AuthChecker authChecker) {
        this.adminService = adminService;
        this.authChecker = authChecker;
    }

    @PostMapping("/grant")
    public R<?> grant(@Valid @RequestBody GrantPointsRequest req) {
        Long adminId = authChecker.requireAdmin().userId();
        return R.ok(adminService.grantPoints(adminId, req.getUserId(), req.getAmount(), req.getRemark(), false));
    }

    @PostMapping("/deduct")
    public R<?> deduct(@Valid @RequestBody GrantPointsRequest req) {
        Long adminId = authChecker.requireAdmin().userId();
        return R.ok(adminService.grantPoints(adminId, req.getUserId(), req.getAmount(), req.getRemark(), true));
    }

    @GetMapping("/users")
    public R<?> users() {
        authChecker.requireAdmin();
        return R.ok(adminService.users());
    }

    @GetMapping("/dashboard")
    public R<?> dashboard() {
        authChecker.requireAdmin();
        return R.ok(adminService.dashboard());
    }
}
