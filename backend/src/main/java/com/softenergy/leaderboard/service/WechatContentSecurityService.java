package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.config.SoftEnergyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(WechatContentSecurityService.class);

    private final SoftEnergyProperties properties;
    private final WechatApiResponseParser responseParser;
    private final RestClient restClient;
    private volatile CachedToken cachedToken;

    @Autowired
    public WechatContentSecurityService(
            SoftEnergyProperties properties,
            WechatApiResponseParser responseParser) {
        this(properties, responseParser, productionRestClient());
    }

    WechatContentSecurityService(
            SoftEnergyProperties properties,
            WechatApiResponseParser responseParser,
            RestClient restClient) {
        this.properties = properties;
        this.responseParser = responseParser;
        this.restClient = restClient;
    }

    private static RestClient productionRestClient() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    public void checkText(String openid, String content) {
        if (!properties.getWechat().isContentSecurityEnabled()) return;
        if (properties.getWechat().isMockEnabled()) return;
        try {
            checkTextWithWechat(openid, content);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            LOGGER.warn("微信内容安全接口请求异常：exception={}", exception.getClass().getSimpleName());
            throw new BusinessException(
                    "CONTENT_SECURITY_UNAVAILABLE", "内容安全检测暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void checkTextWithWechat(String openid, String content) {
        if (openid == null || openid.isBlank()) {
            throw new BusinessException(
                    "CONTENT_SECURITY_UNAVAILABLE", "内容安全检测暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }

        String token = accessToken();
        Map<String, Object> response = requestTextCheck(token, openid, content);
        if (isInvalidAccessToken(response)) {
            LOGGER.warn("微信内容安全 Access Token 被拒绝，清除缓存后重试：errcode={}",
                    number(response.get("errcode")));
            invalidateAccessToken(token);
            response = requestTextCheck(accessToken(), openid, content);
        }

        if (number(response.get("errcode")) != 0) {
            logWechatFailure("msg_sec_check", response);
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

    private Map<String, Object> requestTextCheck(String token, String openid, String content) {
        String responseBody = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("api.weixin.qq.com").path("/wxa/msg_sec_check")
                        .queryParam("access_token", token)
                        .build())
                .body(Map.of(
                        "content", content,
                        "version", 2,
                        "scene", 2,
                        "openid", openid))
                .exchange((request, response) -> response.bodyTo(String.class));
        return responseParser.parse(responseBody);
    }

    private boolean isInvalidAccessToken(Map<String, Object> response) {
        long errorCode = number(response.get("errcode"));
        return errorCode == 40001 || errorCode == 40014 || errorCode == 42001;
    }

    private void invalidateAccessToken(String rejectedToken) {
        synchronized (this) {
            if (cachedToken != null && cachedToken.value().equals(rejectedToken)) {
                cachedToken = null;
            }
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
        String responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("api.weixin.qq.com").path("/cgi-bin/token")
                        .queryParam("grant_type", "client_credential")
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .build())
                .exchange((request, response) -> response.bodyTo(String.class));
        Map<String, Object> response = responseParser.parse(responseBody);
        if (response.get("access_token") == null) {
            logWechatFailure("access_token", response);
            throw new BusinessException(
                    "CONTENT_SECURITY_UNAVAILABLE", "内容安全检测暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
        long expiresIn = Math.max(300, number(response.get("expires_in")));
        cachedToken = new CachedToken(String.valueOf(response.get("access_token")), Instant.now().plusSeconds(expiresIn));
        return cachedToken.value();
    }

    private void logWechatFailure(String stage, Map<String, Object> response) {
        LOGGER.warn(
                "微信内容安全接口返回失败：stage={}, errcode={}, errmsg={}, traceId={}",
                stage,
                number(response.get("errcode")),
                safeLogValue(response.get("errmsg")),
                safeLogValue(response.get("trace_id")));
    }

    private String safeLogValue(Object value) {
        if (value == null) return "";
        String sanitized = String.valueOf(value).replace('\r', ' ').replace('\n', ' ');
        return sanitized.substring(0, Math.min(sanitized.length(), 200));
    }

    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception ignored) { return -1; }
    }

    private record CachedToken(String value, Instant expiresAt) {}
}
