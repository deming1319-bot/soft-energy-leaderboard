package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.AuthDtos;
import com.softenergy.leaderboard.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final AdminAuthService service;

    public AdminAuthController(AdminAuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.AdminLoginRequest request) {
        return ApiResponse.success("登录成功", service.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.CurrentUser> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.current(jwt.getSubject()));
    }
}

