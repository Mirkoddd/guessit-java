package io.guessit.rules.numerals;

import com.mirkoddd.sift.core.engine.SiftCompiledPattern;
import io.guessit.core.text.patterns.RomanNumeralPatterns;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Handles Roman Numbers parsing.
 * Pure stateless parser without enforced Singleton pattern.
 */
final class RomanNumerals implements TokenNumeralParser {

    private static final SiftCompiledPattern FULL_PATTERN = RomanNumeralPatterns.buildFullPattern();

    RomanNumerals() {
    }

    private enum RomanSymbol {
        I(1), V(5), X(10), L(50), C(100), D(500), M(1000);

        final int value;

        RomanSymbol(int value) {
            this.value = value;
        }

        char asChar() {
            return name().charAt(0);
        }

        private static final int[] LOOKUP = new int[128];

        static {
            for (RomanSymbol symbol : values()) {
                LOOKUP[symbol.asChar()] = symbol.value;
            }
        }

        static int getValue(char c) {
            return (c < 128) ? LOOKUP[c] : 0;
        }
    }

    @Override
    public Integer tryParse(List<String> words) {
        return words.stream()
                .map(word -> parse(word.toUpperCase(Locale.ROOT)))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    static Integer parse(String value) {
        if (!FULL_PATTERN.matchesEntire(value)) return null;

        int total = 0;
        int rightValue = 0;

        for (int i = value.length() - 1; i >= 0; i--) {
            int currentValue = RomanSymbol.getValue(value.charAt(i));

            boolean isSubtractive = currentValue < rightValue;
            total += isSubtractive ? -currentValue : currentValue;

            rightValue = currentValue;
        }

        return total;
    }
}