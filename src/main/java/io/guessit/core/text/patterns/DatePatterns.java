package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.NamedCapture;
import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class DatePatterns {
    private DatePatterns() {
    }

    public static Pattern buildCompact8digit(String fullDateGroup) {
        return buildCompactPattern(8, fullDateGroup);
    }

    public static Pattern buildCompact6digit(String fullDateGroup) {
        return buildCompactPattern(6, fullDateGroup);
    }

    private static Pattern buildCompactPattern(int amount, String fullDateGroup) {
        var captureAmountDigits = capture(fullDateGroup, exactly(amount).digits());

        var pattern = find(dateSeparator())
                .followedBy(namedCapture(captureAmountDigits))
                .followedBy(dateSeparator());

        return compileCaseInsensitive(pattern);
    }

    public static Pattern build2digitsStart(
            String fullDateGroup, String part1Group,
            String part2Group, String part3Group
    ) {
        var firstPart2digits = captureAmountOfDigitsFromPart(2, part1Group);
        var secondPart1to2digits = capture1to2digitsFromPart(part2Group);
        var thirdPart1to2digits = capture1to2digitsFromPart(part3Group);

        var pattern = namedCapture(firstPart2digits)
                .followedBy(dateSeparator())
                .followedBy(namedCapture(secondPart1to2digits))
                .followedBy(dateSeparator())
                .followedBy(namedCapture(thirdPart1to2digits));

        return compileBounded(fullDateGroup, pattern);
    }

    public static Pattern build1to2digitsStart(
            String fullDateGroup, String part1Group,
            String part2Group, String part3Group
    ) {
        var firstPart1to2digits = capture1to2digitsFromPart(part1Group);
        var secondPart1to2digits = capture1to2digitsFromPart(part2Group);
        var thirdPart2digits = captureAmountOfDigitsFromPart(2, part3Group);

        var pattern = namedCapture(firstPart1to2digits)
                .followedBy(dateSeparator())
                .followedBy(namedCapture(secondPart1to2digits))
                .followedBy(dateSeparator())
                .followedBy(namedCapture(thirdPart2digits));

        return compileBounded(fullDateGroup, pattern);
    }

    public static Pattern build4digitsStart(
            String fullDateGroup, String part1Group,
            String part2Group, String part3Group
    ) {
        var firstPart4digits = captureAmountOfDigitsFromPart(4, part1Group);
        var secondPart1to2digits = capture1to2digitsFromPart(part2Group);
        var thirdPart1to2digits = capture1to2digitsFromPart(part3Group);

        var pattern = namedCapture(firstPart4digits)
                .followedBy(extendedSeparator())
                .followedBy(namedCapture(secondPart1to2digits))
                .followedBy(dateSeparator())
                .followedBy(namedCapture(thirdPart1to2digits));

        return compileBounded(fullDateGroup, pattern);
    }

    public static Pattern build4digitsEnd(
            String fullDateGroup, String part1Group,
            String part2Group, String part3Group
    ) {
        var firstPart1to2digits = capture1to2digitsFromPart(part1Group);
        var secondPart1to2digits = capture1to2digitsFromPart(part2Group);
        var thirdPart4digits = captureAmountOfDigitsFromPart(4, part3Group);

        var pattern = namedCapture(firstPart1to2digits)
                .followedBy(dateSeparator())
                .followedBy(namedCapture(secondPart1to2digits))
                .followedBy(extendedSeparator())
                .followedBy(namedCapture(thirdPart4digits));

        return compileBounded(fullDateGroup, pattern);
    }

    public static Pattern buildMonthWord(
            String fullDateGroup, String part1Group,
            String part2Group, String part3Group
    ) {
        var firstPart1to2digits = capture1to2digitsFromPart(part1Group);
        var secondPart3to10letters = capture(part2Group, between(3, 10).letters());
        var thirdPart4digits = captureAmountOfDigitsFromPart(4, part3Group);

        var optionalOrdinalSuffix = optional().of(anyOfStrings("st", "nd", "rd", "th"));

        var pattern = namedCapture(firstPart1to2digits)
                .followedBy(optionalOrdinalSuffix)
                .followedBy(dateSeparator())
                .followedBy(namedCapture(secondPart3to10letters))
                .followedBy(dateSeparator())
                .followedBy(namedCapture(thirdPart4digits));

        return compileBounded(fullDateGroup, pattern);
    }

    private static NamedCapture captureAmountOfDigitsFromPart(int amount, String part) {
        return capture(part, exactly(amount).digits());
    }

    private static NamedCapture capture1to2digitsFromPart(String part) {
        return capture(part, between(1, 2).digits());
    }

    private static Pattern compileBounded(String fullDateGroup, SiftPattern<Fragment> innerSequence) {
        var captureFullDate = capture(fullDateGroup, innerSequence);

        var boundedPattern = namedCapture(captureFullDate)
                .notPrecededBy(exactly(1).digits())
                .notFollowedBy(exactly(1).digits());
        return compileCaseInsensitive(boundedPattern);
    }

    private static SiftPattern<Fragment> dateSeparator() {
        return anyOfCharacters('-', '/', '.', ' ');
    }

    private static SiftPattern<Fragment> extendedSeparator() {
        return anyOfCharacters('-', '/', '.', ' ', 'x');
    }
}
