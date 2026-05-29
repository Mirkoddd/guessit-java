package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.zeroOrMore;
import static io.guessit.core.text.patterns.CommonPatterns.anyOfCharacters;

public final class WeakCommonPatterns {
    private WeakCommonPatterns() {}

    public static Pattern buildRangePattern() {
        var spacing = anyOfCharacters(' ', '.', '_');
        var optionalSpacing = zeroOrMore().of(spacing);

        var rangeDelimiter = anyOfCharacters('-', '~');

        var rangePattern = optionalSpacing
                .followedBy(rangeDelimiter)
                .followedBy(optionalSpacing);

        return Pattern.compile(rangePattern.shake());
    }
}
