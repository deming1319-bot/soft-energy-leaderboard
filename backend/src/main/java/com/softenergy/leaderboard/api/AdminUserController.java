package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.UserDtos;
import com.softenergy.leaderboard.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final UserService service;

    public AdminUserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<UserDtos.UserListItem>> list() {
        return ApiResponse.success(service.list());
    }
}

