package com.lazybeartoby.btcmarket.service;

import com.lazybeartoby.btcmarket.common.constant.AppConstants;
import com.lazybeartoby.btcmarket.common.exception.BizException;
import com.lazybeartoby.btcmarket.common.security.JwtUtil;
import com.lazybeartoby.btcmarket.model.dto.LoginRequest;
import com.lazybeartoby.btcmarket.model.dto.RegisterRequest;
import com.lazybeartoby.btcmarket.model.entity.User;
import com.lazybeartoby.btcmarket.model.vo.UserVO;
import com.lazybeartoby.btcmarket.repository.UserRepository;
import com.lazybeartoby.btcmarket.repository.WalletRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(AppConstants.ROLE_USER);
        user.setStatus(1);
        user = userRepository.save(user);

        // init wallet with welcome bonus
        var wallet = new com.lazybeartoby.btcmarket.model.entity.Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(new java.math.BigDecimal("10000"));
        wallet.setFrozenBalance(java.math.BigDecimal.ZERO);
        walletRepository.save(wallet);

        return buildLoginResult(user);
    }

    public Map<String, Object> login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException("账号已被禁用");
        }
        return buildLoginResult(user);
    }

    private Map<String, Object> buildLoginResult(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toVO(user));
        return result;
    }

    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
    }
}
