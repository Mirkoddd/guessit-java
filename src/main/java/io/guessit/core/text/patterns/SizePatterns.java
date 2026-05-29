package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class SizePatterns {

    private SizePatterns() {}

    public static Pattern buildSizePattern(String sizeGroupName) {
        var oneOrMoreDigits = oneOrMore().digits();

        var decimalPart = exactly(1).character('.')
                .followedBy(oneOrMoreDigits);

        var optionalDecimalClause = optional().of(decimalPart);
        var optionalDash = optional().character('-');
        var units = anyOfStrings("mb", "gb", "tb");

        var sizeValueClause = oneOrMoreDigits
                .followedBy(optionalDecimalClause)
                .followedBy(optionalDash, units);

        var sizeValueGroup = capture(sizeGroupName, sizeValueClause);

        var basePattern = namedCapture(sizeValueGroup);

        return compileCaseInsensitive(basePattern);
    }
}