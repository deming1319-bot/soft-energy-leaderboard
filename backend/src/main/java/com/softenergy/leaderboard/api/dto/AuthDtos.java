package com.softenergy.leaderboard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record AdminLoginRequest(
            @NotBlank(message = "请输入管理员账号") String username,
            @NotBlank(message = "请输入管理员密码") String password) {}

    public record MiniappLoginRequest(
            @NotBlank(message = "缺少微信登录凭证") String code,
            @Size(max = 64) String deviceId,
            @AssertTrue(message = "请先阅读并同意用户协议与隐私保护指引") Boolean agreementAccepted,
            @NotBlank(message = "缺少隐私版本") @Size(max = 32) String privacyVersion,
            @NotBlank(message = "缺少协议版本") @Size(max = 32) String termsVersion) {}

    public record TestCenterSessionRequest(
            @NotBlank(message = "请输入测试学员名称") @Size(max = 24) String nickname,
            @NotBlank(message = "缺少测试身份标识") @Size(min = 8, max = 64) String testerKey) {}

    public record TokenResponse(
            String accessToken,
            Instant expiresAt,
            String tokenType,
            CurrentUser currentUser) {}

    public record CurrentUser(
            String id,
            String username,
            String displayName,
            String avatarUrl,
            String role,
            String maskedPhone,
            boolean profileComplete) {}
}
