package com.softenergy.leaderboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "soft-energy")
public class SoftEnergyProperties {
    private Security security = new Security();
    private Cors cors = new Cors();
    private Wechat wechat = new Wechat();
    private Bootstrap bootstrap = new Bootstrap();

    @Getter
    @Setter
    public static class Security {
        private String jwtSecretBase64;
        private String jwtIssuer;
        private long tokenTtlMinutes = 720;
        private String phoneKeyBase64;
        private String phoneHmacBase64;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Wechat {
        private boolean mockEnabled;
        private boolean contentSecurityEnabled;
        private String appId;
        private String appSecret;
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String adminUsername;
        private String adminPassword;
        private String adminDisplayName;
        private boolean demoDataEnabled;
    }
}
