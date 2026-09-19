package com.softenergy.leaderboard.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestAccountCatalogTest {
    private final TestAccountCatalog catalog = new TestAccountCatalog();

    @Test
    void exposesTenStableChineseTestAccounts() {
        assertThat(catalog.accounts()).hasSize(10);
        assertThat(catalog.accounts()).extracting(TestAccountCatalog.TestAccount::testerKey).doesNotHaveDuplicates();
        assertThat(catalog.accounts()).extracting(TestAccountCatalog.TestAccount::nickname)
                .containsExactly("张明", "李静", "王磊", "刘芳", "陈伟", "杨敏", "赵强", "黄欣", "周宁", "吴悦");
    }
}
