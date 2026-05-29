package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.exactly;

public final class YearPatterns {
    private YearPatterns() {}

    public static Pattern buildYearPattern() {
        var pattern = exactly(4).digits();
        return Pattern.compile(pattern.shake());
    }
}
