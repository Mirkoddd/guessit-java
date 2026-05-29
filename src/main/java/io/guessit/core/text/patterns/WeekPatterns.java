package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static com.mirkoddd.sift.core.SiftPatterns.literal;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class WeekPatterns {
    private WeekPatterns() {}

    public static Pattern buildPattern(String weekGroupName) {
        var separatorChar = anyOfCharacters(' ', '.', '_', '-');
        var weekKeyword = literal("week");

        var optionalSeparators = zeroOrMore().of(separatorChar);
        var weekNumberCapture = capture(weekGroupName, between(1, 2).digits());

        var weekPatternTree = find(weekKeyword)
                .followedBy(optionalSeparators)
                .followedBy(namedCapture(weekNumberCapture));

        return compileCaseInsensitive(weekPatternTree);
    }
}