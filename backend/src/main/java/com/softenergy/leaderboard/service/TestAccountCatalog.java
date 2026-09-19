package com.softenergy.leaderboard.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestAccountCatalog {
    private static final List<TestAccount> ACCOUNTS = List.of(
            new TestAccount("preset-student-01", "张明"),
            new TestAccount("preset-student-02", "李静"),
            new TestAccount("preset-student-03", "王磊"),
            new TestAccount("preset-student-04", "刘芳"),
            new TestAccount("preset-student-05", "陈伟"),
            new TestAccount("preset-student-06", "杨敏"),
            new TestAccount("preset-student-07", "赵强"),
            new TestAccount("preset-student-08", "黄欣"),
            new TestAccount("preset-student-09", "周宁"),
            new TestAccount("preset-student-10", "吴悦")
    );

    public List<TestAccount> accounts() {
        return ACCOUNTS;
    }

    public record TestAccount(String testerKey, String nickname) {}
}
