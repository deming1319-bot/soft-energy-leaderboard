package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.domain.repository.AdminAuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditController {
    private final AdminAuditLogRepository repository;

    public AdminAuditController(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        var rows = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100)).stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.getId(),
                        "adminUsername", item.getAdminUsername(),
                        "action", item.getAction(),
                        "targetType", item.getTargetType(),
                        "targetId", item.getTargetId() == null ? "" : item.getTargetId(),
                        "summary", item.getSummary(),
                        "createdAt", item.getCreatedAt()))
                .toList();
        return ApiResponse.success(rows);
    }
}

