package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.AuthDtos;
import com.softenergy.leaderboard.service.TestAccountCatalog;
import com.softenergy.leaderboard.service.WechatAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/test-center")
public class AdminTestCenterController {
    private final WechatAuthService service;
    private final TestAccountCatalog accountCatalog;

    public AdminTestCenterController(WechatAuthService service, TestAccountCatalog accountCatalog) {
        this.service = service;
        this.accountCatalog = accountCatalog;
    }

    @GetMapping("/accounts")
    public ApiResponse<List<TestAccountCatalog.TestAccount>> accounts() {
        return ApiResponse.success(accountCatalog.accounts());
    }

    @PostMapping("/session")
    public ApiResponse<AuthDtos.TokenResponse> session(
            @Valid @RequestBody AuthDtos.TestCenterSessionRequest request) {
        return ApiResponse.success("测试学员已就绪", service.testCenterLogin(request));
    }
}
