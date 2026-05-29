package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;
import io.guessit.core.text.Seps;

import static io.guessit.core.text.patterns.CommonPatterns.anyOfCharacters;

public final class AbbreviationsPatterns {

    private AbbreviationsPatterns() {}

    static final SiftPattern<Fragment> SEPS_NO_FS_PATTERN = buildSepsPattern();

    private static SiftPattern<Fragment> buildSepsPattern() {
        StringBuilder validSeps = new StringBuilder();

        for (char c : Seps.CHARS.toCharArray()) {
            if (c != '/' && c != '\\') {
                validSeps.append(c);
            }
        }

        return anyOfCharacters(validSeps.toString().toCharArray());
    }

    public static final String SEPS_REPLACEMENT = SEPS_NO_FS_PATTERN.shake();
}