package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.api.dto.AuthDtos;
import com.softenergy.leaderboard.domain.model.AdminUser;
import com.softenergy.leaderboard.domain.repository.AdminUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MINUTES = 10;
    private final AdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ConcurrentHashMap<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public AdminAuthService(
            AdminUserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.AdminLoginRequest request) {
        String username = request.username().trim();
        ensureNotLocked(username);
        AdminUser admin = repository.findByUsername(username)
                .filter(AdminUser::isEnabled)
                .orElseThrow(() -> loginFailed(username));
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw loginFailed(username);
        }
        attempts.remove(username);
        admin.setLastLoginAt(Instant.now());
        JwtService.IssuedToken token = jwtService.issue(
                admin.getUsername(), "ADMIN", "ADMIN", admin.getDisplayName());
        return new AuthDtos.TokenResponse(
                token.value(), token.expiresAt(), "Bearer",
                new AuthDtos.CurrentUser(
                        String.valueOf(admin.getId()), admin.getUsername(), admin.getDisplayName(),
                        null, "ADMIN", null, true));
    }

    private void ensureNotLocked(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null || attempt.lockedUntil() == null) return;
        if (attempt.lockedUntil().isAfter(Instant.now())) {
            throw new BusinessException("LOGIN_RATE_LIMITED", "登录失败次数过多，请10分钟后再试", HttpStatus.TOO_MANY_REQUESTS);
        }
        attempts.remove(username);
    }

    private BusinessException loginFailed(String username) {
        attempts.compute(username, (key, current) -> {
            int failures = current == null ? 1 : current.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES
                    ? Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES) : null;
            return new LoginAttempt(failures, lockedUntil);
        });
        return new BusinessException("LOGIN_FAILED", "账号或密码不正确", HttpStatus.UNAUTHORIZED);
    }

    private record LoginAttempt(int failures, Instant lockedUntil) {}

    @Transactional(readOnly = true)
    public AuthDtos.CurrentUser current(String username) {
        AdminUser admin = repository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("管理员账号不存在"));
        return new AuthDtos.CurrentUser(
                String.valueOf(admin.getId()), admin.getUsername(), admin.getDisplayName(),
                null, "ADMIN", null, true);
    }
}
