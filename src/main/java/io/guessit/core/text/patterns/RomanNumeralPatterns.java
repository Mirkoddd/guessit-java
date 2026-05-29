package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;
import com.mirkoddd.sift.core.engine.SiftCompiledPattern;

import java.util.List;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.anyOf;
import static com.mirkoddd.sift.core.SiftPatterns.literal;
import static io.guessit.core.text.patterns.CommonPatterns.anyOfCharacters;

public final class RomanNumeralPatterns {

    private RomanNumeralPatterns() {}

    public static SiftCompiledPattern buildFullPattern() {
        var basePattern = buildBasePattern();

        return fromStart()
                .of(basePattern)
                .andNothingElse()
                .sieve();
    }

    public static SiftPattern<Fragment> buildBasePattern() {
        var thousands = between(0, 4).character('M');
        var hundreds = buildDigitGroup('C', 'D', 'M');
        var tens = buildDigitGroup('X', 'L', 'C');
        var ones = buildDigitGroup('I', 'V', 'X');

        var validRomanChars = anyOfCharacters('I', 'V', 'X', 'L', 'C', 'D', 'M');
        var oneOrMoreOfValidRomanChars = oneOrMore().of(validRomanChars);

        return fromAnywhere()
                .mustBeFollowedBy(oneOrMoreOfValidRomanChars)
                .followedBy(List.of(thousands, hundreds, tens, ones));
    }

    private static SiftPattern<Fragment> buildDigitGroup(char base, char mid, char top) {
        String highSubtractive = String.valueOf(base) + top;
        String lowSubtractive = String.valueOf(base) + mid;

        return anyOf(
                literal(highSubtractive),
                literal(lowSubtractive),
                optional().character(mid)
                        .then().between(0, 3).character(base)
        );
    }
}