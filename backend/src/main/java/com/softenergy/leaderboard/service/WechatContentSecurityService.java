package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.config.SoftEnergyProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class WechatContentSecurityService {
    private final SoftEnergyProperties properties;
    private final RestClient restClient;
    private volatile CachedToken cachedToken;

    public WechatContentSecurityService(SoftEnergyProperties properties) {
        this.properties = properties;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public void checkText(String openid, String content) {
        if (!properties.getWechat().isContentSecurityEnabled()) return;
        if (properties.getWechat().isMockEnabled()) return;
        try {
            checkTextWithWechat(openid, content);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(
                    "CONTENT_SECURITY_UNAVAILABLE", "内容安全检测暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void checkTextWithWechat(String openid, String content) {
        if (openid == null || openid.isBlank()) {
            throw new BusinessException(
                    "CONTENT_SECURITY_UNAVAILABLE", "内容安全检测暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("api.weixin.qq.com").path("/wxa/msg_sec_check")
                        .queryParam("access_token", accessToken())
                        .build())
                .body(Map.of(
                        "content", content,
                        "version", 2,
                        "scene", 2,
                        "openid", openid))
                .retrieve()
                .body(Map.class);

        if (response == null || number(response.get("errcode")) != 0) {
            throw new BusinessException(
                    "CONTENT_SECURITY_UNAVAILABLE", "内容安全检测暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Object resultValue = response.get("result");
        String suggest = resultValue instanceof Map<?, ?> result
                ? String.valueOf(result.get("suggest")) : "";
        if (!"pass".equalsIgnoreCase(suggest)) {
            throw BusinessException.badRequest("CONTENT_RISK", "内容未通过安全检测，请调整后再试");
        }
    }

    private String accessToken() {
        CachedToken current = cachedToken;
        Instant now = Instant.now();
        if (current != null && current.expiresAt().isAfter(now.plusSeconds(60))) return current.value();
        synchronized (this) {
            current = cachedToken;
            if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(60))) return current.value();
            return refreshAccessToken();
        }
    }

    private String refreshAccessToken() {
        String appId = properties.getWechat().getAppId();
        String appSecret = properties.getWechat().getAppSecret();
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new BusinessException(
                    "CONTENT_SECURITY_UNAVAILABLE", "内容安全检测尚未配置", HttpStatus.SERVICE_UNAVAILABLE);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("api.weixin.qq.com").path("/cgi-bin/token")
                        .queryParam("grant_type", "client_credential")
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .build())
                .retrieve()
                .body(Map.class);
        if (response == null || response.get("access_token") == null) {
            throw new BusinessException(
                    "CONTENT_SECURITY_UNAVAILABLE", "内容安全检测暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
        long expiresIn = Math.max(300, number(response.get("expires_in")));
        cachedToken = new CachedToken(String.valueOf(response.get("access_token")), Instant.now().plusSeconds(expiresIn));
        return cachedToken.value();
    }

    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception ignored) { return -1; }
    }

    private record CachedToken(String value, Instant expiresAt) {}
}
