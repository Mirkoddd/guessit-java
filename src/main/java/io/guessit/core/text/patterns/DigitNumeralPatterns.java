package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;
import com.mirkoddd.sift.core.engine.SiftCompiledPattern;

import static com.mirkoddd.sift.core.Sift.between;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.namedCapture;

public final class DigitNumeralPatterns {

    private DigitNumeralPatterns() {}

    public static SiftCompiledPattern buildDigitPattern(String groupName) {
        var numberCapture = capture(groupName, buildBasePattern());

        return namedCapture(numberCapture).sieve();
    }

    public static SiftPattern<Fragment> buildBasePattern() {
        return between(1, 4).digits();
    }
}