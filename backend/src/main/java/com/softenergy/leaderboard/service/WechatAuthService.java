package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.api.dto.AuthDtos;
import com.softenergy.leaderboard.config.SoftEnergyProperties;
import com.softenergy.leaderboard.domain.model.MiniappUser;
import com.softenergy.leaderboard.domain.model.UserStatus;
import com.softenergy.leaderboard.domain.repository.MiniappUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Service
public class WechatAuthService {
    private final MiniappUserRepository repository;
    private final SoftEnergyProperties properties;
    private final JwtService jwtService;
    private final PhoneCryptoService phoneCryptoService;
    private final RestClient restClient = RestClient.create();

    public WechatAuthService(
            MiniappUserRepository repository,
            SoftEnergyProperties properties,
            JwtService jwtService,
            PhoneCryptoService phoneCryptoService) {
        this.repository = repository;
        this.properties = properties;
        this.jwtService = jwtService;
        this.phoneCryptoService = phoneCryptoService;
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.MiniappLoginRequest request) {
        WechatIdentity identity = exchange(request);
        return signIn(identity, null, null, request.privacyVersion(), request.termsVersion());
    }

    @Transactional
    public AuthDtos.TokenResponse testCenterLogin(AuthDtos.TestCenterSessionRequest request) {
        String nickname = request.nickname().trim();
        if (!nickname.startsWith("测试学员·")) nickname = "测试学员·" + nickname;
        WechatIdentity identity = new WechatIdentity(
                "test-center:" + sha256(request.testerKey().trim()).substring(0, 24), null);
        return signIn(identity, nickname, null, null, null);
    }

    private AuthDtos.TokenResponse signIn(
            WechatIdentity identity,
            String nickname,
            String avatarUrl,
            String privacyVersion,
            String termsVersion) {
        MiniappUser user = repository.findByOpenid(identity.openid())
                .orElseGet(MiniappUser::new);
        if (user.getOpenid() == null) user.setOpenid(identity.openid());
        if (identity.unionid() != null) user.setUnionid(identity.unionid());
        if (nickname != null && !nickname.isBlank()) user.setNickname(nickname.trim());
        if (user.getNickname() == null || user.getNickname().isBlank()) user.setNickname("修习者");
        if (avatarUrl != null && !avatarUrl.isBlank()) user.setAvatarUrl(avatarUrl);
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("ACCOUNT_DISABLED", "账号已停用", HttpStatus.FORBIDDEN);
        }
        Instant now = Instant.now();
        if (privacyVersion != null && termsVersion != null) {
            user.setPrivacyVersion(privacyVersion);
            user.setTermsVersion(termsVersion);
            user.setConsentedAt(now);
        }
        user.setLastLoginAt(now);
        repository.save(user);
        JwtService.IssuedToken token = jwtService.issue(user.getId(), "USER", "MINIAPP", user.getNickname());
        return new AuthDtos.TokenResponse(
                token.value(), token.expiresAt(), "Bearer",
                new AuthDtos.CurrentUser(
                        user.getId(), null, user.getNickname(), user.getAvatarUrl(), "USER",
                        phoneCryptoService.maskEncrypted(user.getPhoneEncrypted()), profileComplete(user)));
    }

    private boolean profileComplete(MiniappUser user) {
        return user.getNickname() != null && !user.getNickname().isBlank()
                && !"修习者".equals(user.getNickname());
    }

    private WechatIdentity exchange(AuthDtos.MiniappLoginRequest request) {
        if (properties.getWechat().isMockEnabled()) {
            if (request.deviceId() == null || request.deviceId().isBlank()) {
                throw new BusinessException("DEVICE_ID_REQUIRED", "开发模拟登录缺少设备标识", HttpStatus.BAD_REQUEST);
            }
            return new WechatIdentity("mock:" + sha256(request.deviceId()).substring(0, 24), null);
        }
        if (properties.getWechat().getAppId() == null || properties.getWechat().getAppId().isBlank()
                || properties.getWechat().getAppSecret() == null || properties.getWechat().getAppSecret().isBlank()) {
            throw new IllegalStateException("生产环境未配置微信 AppID/AppSecret");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("api.weixin.qq.com").path("/sns/jscode2session")
                        .queryParam("appid", properties.getWechat().getAppId())
                        .queryParam("secret", properties.getWechat().getAppSecret())
                        .queryParam("js_code", request.code())
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .retrieve()
                .body(Map.class);
        if (response == null || response.get("openid") == null) {
            throw new BusinessException("WECHAT_LOGIN_FAILED", "微信登录暂时不可用", HttpStatus.BAD_GATEWAY);
        }
        return new WechatIdentity(String.valueOf(response.get("openid")),
                response.get("unionid") == null ? null : String.valueOf(response.get("unionid")));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record WechatIdentity(String openid, String unionid) {}
}
