package com.softenergy.leaderboard.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "miniapp_users")
public class MiniappUser {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, length = 128)
    private String openid;

    @Column(length = 128)
    private String unionid;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Lob
    @Column(name = "phone_encrypted", columnDefinition = "TEXT")
    private String phoneEncrypted;

    @Column(name = "phone_hash", length = 64)
    private String phoneHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "privacy_version", length = 32)
    private String privacyVersion;

    @Column(name = "terms_version", length = 32)
    private String termsVersion;

    @Column(name = "consented_at")
    private Instant consentedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
