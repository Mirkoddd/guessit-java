package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.fromAnywhere;
import static com.mirkoddd.sift.core.Sift.oneOrMore;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.namedCapture;

public final class ProperCountPatterns {

    private ProperCountPatterns() {}

    public static Pattern buildNonAlphanumericPattern() {
        var pattern = oneOrMore().nonAlphanumeric().preventBacktracking();
        return Pattern.compile(pattern.shake());
    }

    public static Pattern buildTrailingDigitsPattern(String digitsGroupName) {
        var digitsCapture = capture(digitsGroupName, oneOrMore().digits());

        var pattern = namedCapture(digitsCapture).andNothingElse();

        return Pattern.compile(pattern.shake());
    }
}