package io.guessit.core.text.patterns;

import io.guessit.rules.numerals.Numerals;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.optional;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.anyOfStrings;
import static io.guessit.core.text.patterns.CommonPatterns.compileCaseInsensitive;
import static io.guessit.core.text.patterns.CommonPatterns.find;

public final class PartPatterns {

    private PartPatterns() {}

    public static Pattern buildPartPattern(String valueGroupName) {
        var partWords = anyOfStrings("part", "pt");
        var optionalNoFsSeparator = optional().of(AbbreviationsPatterns.SEPS_NO_FS_PATTERN);

        var partValueGroup = capture(valueGroupName, Numerals.NUMERAL_PATTERN);

        var basePattern = find(partWords)
                .followedBy(optionalNoFsSeparator)
                .then().namedCapture(partValueGroup);

        return compileCaseInsensitive(basePattern);
    }
}