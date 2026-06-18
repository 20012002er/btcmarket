package com.lazybeartoby.btcmarket.service;

import com.lazybeartoby.btcmarket.model.entity.User;
import com.lazybeartoby.btcmarket.model.vo.UserVO;
import com.lazybeartoby.btcmarket.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public UserVO profile(Long userId) {
        return authService.toVO(authService.getUser(userId));
    }

    public List<UserVO> allUsers() {
        return userRepository.findAll().stream().map(authService::toVO).toList();
    }
}
