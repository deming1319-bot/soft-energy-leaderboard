package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.domain.model.SubmissionGrade;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class JudgmentService {
    private static final BigDecimal REVERSED_ORDER_SCORE = new BigDecimal("0.9000");

    public JudgmentResult judge(
            String answer,
            String standardAnswer,
            List<String> answerPoints,
            BigDecimal ignoredRunnerUpThreshold) {
        String normalizedAnswer = normalize(answer);
        String normalizedStandard = normalize(standardAnswer);
        List<String> completePoints = List.copyOf(answerPoints);

        if (normalizedAnswer.equals(normalizedStandard)) {
            return new JudgmentResult(
                    SubmissionGrade.CHAMPION, BigDecimal.ONE,
                    completePoints, normalizedAnswer, true);
        }

        String reversedStandard = reversedStandard(normalizedStandard, answerPoints);
        if (reversedStandard != null && normalizedAnswer.equals(reversedStandard)) {
            return new JudgmentResult(
                    SubmissionGrade.RUNNER_UP, REVERSED_ORDER_SCORE,
                    completePoints, normalizedAnswer, false);
        }

        return new JudgmentResult(
                SubmissionGrade.UNRANKED, BigDecimal.ZERO,
                List.of(), normalizedAnswer, false);
    }

    /**
     * 精确判题只忽略表情符号，并统一 Windows/macOS 换行符。
     * 文字、数字、空格、标点、字数和先后顺序都保留参与比较。
     */
    public String normalize(String value) {
        if (value == null) return "";
        String lineNormalized = value.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder result = new StringBuilder(lineNormalized.length());
        for (int offset = 0; offset < lineNormalized.length();) {
            int codePoint = lineNormalized.codePointAt(offset);
            int nextOffset = offset + Character.charCount(codePoint);
            if (!startsKeycapSequence(lineNormalized, offset, codePoint) && !isEmojiCodePoint(codePoint)) {
                result.appendCodePoint(codePoint);
            }
            offset = nextOffset;
        }
        return result.toString();
    }

    private String reversedStandard(String normalizedStandard, List<String> answerPoints) {
        String[] lines = normalizedStandard.split("\n", -1);
        if (lines.length == 2 && !lines[0].isEmpty() && !lines[1].isEmpty()) {
            return lines[1] + "\n" + lines[0];
        }
        if (answerPoints.size() != 2) return null;

        String first = normalize(answerPoints.get(0));
        String second = normalize(answerPoints.get(1));
        if (first.isEmpty() || second.isEmpty()) return null;
        int firstIndex = normalizedStandard.indexOf(first);
        int secondIndex = normalizedStandard.indexOf(second, firstIndex + first.length());
        if (firstIndex < 0 || secondIndex < 0) return null;

        String prefix = normalizedStandard.substring(0, firstIndex);
        String separator = normalizedStandard.substring(firstIndex + first.length(), secondIndex);
        String suffix = normalizedStandard.substring(secondIndex + second.length());
        return prefix + second + separator + first + suffix;
    }

    private boolean startsKeycapSequence(String value, int offset, int codePoint) {
        if (!(codePoint == '#' || codePoint == '*' || Character.isDigit(codePoint))) return false;
        int cursor = offset + Character.charCount(codePoint);
        if (cursor < value.length()) {
            int next = value.codePointAt(cursor);
            if (next == 0xFE0E || next == 0xFE0F) cursor += Character.charCount(next);
        }
        return cursor < value.length() && value.codePointAt(cursor) == 0x20E3;
    }

    private boolean isEmojiCodePoint(int codePoint) {
        return (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x26FF)
                || (codePoint >= 0x2700 && codePoint <= 0x27BF)
                || (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF)
                || codePoint == 0x200D
                || codePoint == 0x20E3
                || codePoint == 0xFE0E
                || codePoint == 0xFE0F
                || codePoint == 0x00A9
                || codePoint == 0x00AE
                || codePoint == 0x203C
                || codePoint == 0x2049
                || codePoint == 0x2122
                || codePoint == 0x2139;
    }

    public record JudgmentResult(
            SubmissionGrade grade,
            BigDecimal score,
            List<String> matchedPoints,
            String normalizedAnswer,
            boolean correctOrder) {}
}
