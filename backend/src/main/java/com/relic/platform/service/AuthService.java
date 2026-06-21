package com.relic.platform.service;

import com.relic.platform.dto.LoginResponse;
import com.relic.platform.entity.User;
import com.relic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");

        LoginResponse response = new LoginResponse();
        response.setToken(token);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setName(user.getName());
        userInfo.setRole(user.getRole());
        userInfo.setPhone(user.getPhone());
        response.setUser(userInfo);

        return response;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        throw new RuntimeException("暂未实现用户上下文获取");
    }
}
