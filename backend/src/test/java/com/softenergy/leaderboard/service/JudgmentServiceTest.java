package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.domain.model.SubmissionGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JudgmentServiceTest {
    private final JudgmentService service = new JudgmentService();
    private final List<String> points = List.of("海纳百川，有容乃大", "壁立千仞，无欲则刚");
    private final String standard = "海纳百川，有容乃大\n壁立千仞，无欲则刚";

    @Test
    void exactAnswerIsChampion() {
        var result = judge(standard);
        assertThat(result.grade()).isEqualTo(SubmissionGrade.CHAMPION);
        assertThat(result.score()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.correctOrder()).isTrue();
    }

    @Test
    void emojisAreIgnoredWithoutChangingTheGrade() {
        var result = judge("海纳百川😊，有容乃大\n壁立千仞👨‍👩‍👧‍👦，无欲则刚✨");
        assertThat(result.grade()).isEqualTo(SubmissionGrade.CHAMPION);
        assertThat(result.score()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void twoExactLinesInReverseOrderAreRunnerUp() {
        var result = judge("壁立千仞，无欲则刚\n海纳百川，有容乃大");
        assertThat(result.grade()).isEqualTo(SubmissionGrade.RUNNER_UP);
        assertThat(result.correctOrder()).isFalse();
        assertThat(result.score()).isEqualByComparingTo("0.9000");
    }

    @Test
    void punctuationDifferenceIsNotAcceptedAsExact() {
        var result = judge("海纳百川；有容乃大\n壁立千仞，无欲则刚");
        assertThat(result.grade()).isEqualTo(SubmissionGrade.UNRANKED);
        assertThat(result.score()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void missingOrAdditionalCharactersAreUnranked() {
        assertThat(judge("海纳百川，有容乃大\n壁立千仞，无欲则").grade())
                .isEqualTo(SubmissionGrade.UNRANKED);
        assertThat(judge(standard + "。").grade())
                .isEqualTo(SubmissionGrade.UNRANKED);
    }

    @Test
    void partialSimilarityNeverCreatesAThirdGrade() {
        var result = judge("海纳百川，有容乃大");
        assertThat(result.grade()).isEqualTo(SubmissionGrade.UNRANKED);
        assertThat(result.grade()).isNotEqualTo(SubmissionGrade.REVIEW_REQUIRED);
    }

    private JudgmentService.JudgmentResult judge(String answer) {
        return service.judge(answer, standard, points, new BigDecimal("0.6000"));
    }
}
