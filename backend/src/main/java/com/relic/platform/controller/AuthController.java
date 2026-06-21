package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.LoginRequest;
import com.relic.platform.dto.LoginResponse;
import com.relic.platform.entity.User;
import com.relic.platform.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            return ApiResponse.success(response);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ApiResponse<User> me() {
        try {
            User user = authService.getCurrentUser();
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
