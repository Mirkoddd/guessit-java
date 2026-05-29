package io.guessit.rules.numerals;

import com.mirkoddd.sift.core.engine.SiftCompiledPattern;
import io.guessit.core.text.patterns.DigitNumeralPatterns;

import static com.mirkoddd.sift.core.SiftPatterns.capture;

/**
 * Handles validation and parsing of standard digital numerals.
 */
final class DigitNumerals implements RawNumeralParser {

    DigitNumerals() {
    }

    private static final String GROUP_NUMBER = "number";

    private static final SiftCompiledPattern DIGIT_PATTERN = DigitNumeralPatterns.buildDigitPattern(GROUP_NUMBER);

    @Override
    public Integer tryParse(String value) {
        String digits = DIGIT_PATTERN.extractGroups(value).get(GROUP_NUMBER);

        if (digits == null) {
            return null;
        }

        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException _) {
            return null;
        }
    }
}