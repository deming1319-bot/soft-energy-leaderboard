package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.domain.model.AdminAuditLog;
import com.softenergy.leaderboard.domain.repository.AdminAuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AdminAuditLogRepository repository;

    public AuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String admin, String action, String targetType, String targetId, String summary) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUsername(admin);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setSummary(summary);
        repository.save(log);
    }
}

