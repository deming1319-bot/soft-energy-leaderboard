package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.UserDtos;
import com.softenergy.leaderboard.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniapp/profile")
public class MiniappProfileController {
    private final UserService service;

    public MiniappProfileController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<UserDtos.MiniappProfile> profile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.profile(jwt.getSubject()));
    }

    @PutMapping
    public ApiResponse<UserDtos.MiniappProfile> update(
            @Valid @RequestBody UserDtos.UpdateMiniappProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("修习名已更新", service.updateProfile(jwt.getSubject(), request));
    }

    @DeleteMapping
    public ApiResponse<Void> cancel(@AuthenticationPrincipal Jwt jwt) {
        service.cancelAccount(jwt.getSubject());
        return ApiResponse.success("账号已注销", (Void) null);
    }
}
