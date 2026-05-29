package io.guessit.rules.date;

import io.guessit.core.text.Seps;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.guessit.core.text.patterns.DatePatterns.*;

final class DateRouter {

    private DateRouter() {}

    private static final String GROUP_FULL_DATE = "date";
    private static final String GROUP_PART_1 = "p1";
    private static final String GROUP_PART_2 = "p2";
    private static final String GROUP_PART_3 = "p3";

    private static final Pattern COMPACT_8_DIGIT = buildCompact8digit(GROUP_FULL_DATE);
    private static final Pattern COMPACT_6_DIGIT = buildCompact6digit(GROUP_FULL_DATE);
    private static final Pattern TWO_DIGIT_START = build2digitsStart(GROUP_FULL_DATE, GROUP_PART_1, GROUP_PART_2,  GROUP_PART_3);
    private static final Pattern ONE_TWO_DIGIT_START = build1to2digitsStart(GROUP_FULL_DATE, GROUP_PART_1, GROUP_PART_2,  GROUP_PART_3);
    private static final Pattern FOUR_DIGIT_START = build4digitsStart(GROUP_FULL_DATE, GROUP_PART_1, GROUP_PART_2,  GROUP_PART_3);
    private static final Pattern FOUR_DIGIT_END = build4digitsEnd(GROUP_FULL_DATE, GROUP_PART_1, GROUP_PART_2,  GROUP_PART_3);
    private static final Pattern MONTH_WORD = buildMonthWord(GROUP_FULL_DATE, GROUP_PART_1, GROUP_PART_2,  GROUP_PART_3);

    private record PatternRoute(Pattern pattern, DateShape shape) {}

    private static final List<PatternRoute> ROUTES = List.of(
            new PatternRoute(COMPACT_8_DIGIT, DateShape.COMPACT_8_DIGIT),
            new PatternRoute(COMPACT_6_DIGIT, DateShape.COMPACT_6_DIGIT),
            new PatternRoute(TWO_DIGIT_START, DateShape.NUMERIC_SEPARATED),
            new PatternRoute(ONE_TWO_DIGIT_START, DateShape.NUMERIC_SEPARATED),
            new PatternRoute(FOUR_DIGIT_START, DateShape.NUMERIC_SEPARATED),
            new PatternRoute(FOUR_DIGIT_END, DateShape.NUMERIC_SEPARATED),
            new PatternRoute(MONTH_WORD, DateShape.MONTH_WORD)
    );

    /**
     * Extracts the first valid match from each pattern that respects the boundaries.
     */
    static List<RawDateMatch> findCandidates(String input) {
        List<RawDateMatch> candidates = new ArrayList<>();

        for (PatternRoute route : ROUTES) {
            Matcher matcher = route.pattern().matcher(input);

            if (matcher.find()) {
                int startIndex = matcher.start(GROUP_FULL_DATE);
                int endIndex = matcher.end(GROUP_FULL_DATE);

                if (areSeparatorsSurrounding(input, startIndex, endIndex)) {
                    List<String> parts = extractParts(matcher, route.shape());
                    String rawDateMatch = matcher.group(GROUP_FULL_DATE);

                    candidates.add(new RawDateMatch(startIndex, endIndex, rawDateMatch, parts, route.shape()));
                }
            }
        }
        return candidates;
    }

    private static List<String> extractParts(Matcher matcher, DateShape shape) {
        if (shape == DateShape.COMPACT_8_DIGIT || shape == DateShape.COMPACT_6_DIGIT) {
            return List.of();
        }

        return List.of(
                matcher.group(GROUP_PART_1),
                matcher.group(GROUP_PART_2),
                matcher.group(GROUP_PART_3)
        );
    }

    private static boolean areSeparatorsSurrounding(String input, int startIndex, int endIndex) {
        boolean isSafeBefore = startIndex == 0 || Seps.isSep(input.charAt(startIndex - 1));
        boolean isSafeAfter = endIndex == input.length() || Seps.isSep(input.charAt(endIndex));
        return isSafeBefore && isSafeAfter;
    }
}