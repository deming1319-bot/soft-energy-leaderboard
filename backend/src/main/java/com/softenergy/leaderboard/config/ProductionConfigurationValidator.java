package com.softenergy.leaderboard.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
public class ProductionConfigurationValidator implements ApplicationRunner {
    private static final String DEFAULT_JWT = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String DEFAULT_PHONE_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String DEFAULT_PHONE_HMAC = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private final SoftEnergyProperties properties;

    public ProductionConfigurationValidator(SoftEnergyProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> issues = new ArrayList<>();
        String adminPassword = properties.getBootstrap().getAdminPassword();
        if (adminPassword == null || adminPassword.length() < 12 || "123456".equals(adminPassword)) {
            issues.add("生产管理员密码必须至少12位，且不能使用本地测试密码123456");
        }
        if (DEFAULT_JWT.equals(properties.getSecurity().getJwtSecretBase64())) {
            issues.add("生产环境不能使用默认 JWT 密钥");
        }
        if (DEFAULT_PHONE_KEY.equals(properties.getSecurity().getPhoneKeyBase64())
                || DEFAULT_PHONE_HMAC.equals(properties.getSecurity().getPhoneHmacBase64())) {
            issues.add("生产环境不能使用默认个人信息加密密钥");
        }
        if (properties.getWechat().isMockEnabled()) issues.add("生产环境必须关闭微信模拟登录");
        if (!properties.getWechat().isContentSecurityEnabled()) issues.add("生产环境必须开启微信内容安全检测");
        if (blank(properties.getWechat().getAppId()) || blank(properties.getWechat().getAppSecret())) {
            issues.add("生产环境必须配置真实微信 AppID 与 AppSecret");
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("生产配置校验失败：" + String.join("；", issues));
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
