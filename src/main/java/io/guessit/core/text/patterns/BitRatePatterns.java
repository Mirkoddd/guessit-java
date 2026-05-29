package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;

import java.util.List;
import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.*;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class BitRatePatterns {

    private BitRatePatterns() {
    }

    public static List<Pattern> buildPatterns(String groupName) {
        var fullUnitSuffix = buildFullUnitSuffix();

        var integerFormat = oneOrMore().digits()
                .followedBy(fullUnitSuffix);

        var decimalDigits = exactly(1).character('.')
                .then().oneOrMore().digits();

        var decimalFormat = oneOrMore().digits()
                .followedBy(decimalDigits, fullUnitSuffix);

        var integerPattern = compilePattern(integerFormat, groupName);
        var decimalPattern = compilePattern(decimalFormat, groupName);

        return List.of(integerPattern, decimalPattern);
    }

    private static SiftPattern<Fragment> buildFullUnitSuffix() {
        var separator = anyOfCharacters(' ', '.', '_', '-');
        var optionalSeparator = optional().of(separator);

        var metricPrefix = anyOfCharacters('k', 'm', 'g');
        var baseRateUnit = anyOfStrings("bps", "bit", "bits");

        return optionalSeparator.followedBy(metricPrefix, baseRateUnit);
    }

    private static Pattern compilePattern(SiftPattern<Fragment> fragment, String groupName) {
        var valueCaptureGroup = capture(groupName, fragment);
        var pattern = namedCapture(valueCaptureGroup);

        return compileCaseInsensitive(pattern);
    }
}