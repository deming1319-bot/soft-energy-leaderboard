package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.AuthDtos;
import com.softenergy.leaderboard.service.WechatAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/miniapp/auth")
public class MiniappAuthController {
    private final WechatAuthService service;

    public MiniappAuthController(WechatAuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.MiniappLoginRequest request) {
        return ApiResponse.success("登录成功", service.login(request));
    }
}

