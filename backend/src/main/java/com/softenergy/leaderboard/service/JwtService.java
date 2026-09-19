package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.config.SoftEnergyProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final SoftEnergyProperties properties;

    public JwtService(JwtEncoder encoder, SoftEnergyProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedToken issue(String subject, String role, String tokenType, String displayName) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getSecurity().getTokenTtlMinutes(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getSecurity().getJwtIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(subject)
                .claim("roles", List.of(role))
                .claim("token_type", tokenType)
                .claim("display_name", displayName)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}

