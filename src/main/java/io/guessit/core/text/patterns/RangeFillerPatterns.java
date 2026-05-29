package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.Sift;
import com.mirkoddd.sift.core.SiftGlobalFlag;
import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;
import com.mirkoddd.sift.core.engine.SiftCompiledPattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.anyOf;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class RangeFillerPatterns {

    private RangeFillerPatterns() {
    }

    public static SiftCompiledPattern buildGapPattern() {
        var dashOrTilde = anyOfCharacters('-', '~');
        var paddingChar = anyOfCharacters(' ', '.', '_');
        var optionalPadding = zeroOrMore().of(paddingChar);

        var basicGap = basicGapPattern(optionalPadding, dashOrTilde);
        var seasonEpisodeGap = seasonEpisodeGapPattern(dashOrTilde);

        var anyValidGap = anyOf(basicGap, seasonEpisodeGap);

        return Sift.filteringWith(SiftGlobalFlag.CASE_INSENSITIVE)
                .fromStart()
                .of(optionalPadding)
                .followedBy(anyValidGap)
                .followedBy(optionalPadding)
                .andNothingElse()
                .sieve();
    }

    private static SiftPattern<Fragment> basicGapPattern(
            SiftPattern<Fragment> optionalPadding,
            SiftPattern<Fragment> dashOrTilde
    ) {
        var intervalIndicators = anyOf(dashOrTilde, anyOfStrings("to", "a"));

        var sOrE = anyOfCharacters('s', 'e');
        var seasonEpisodeSuffix = find(optionalPadding).followedBy(sOrE);
        var optionalSeasonEpisodeSuffix = optional().of(seasonEpisodeSuffix);

        return find(intervalIndicators).followedBy(optionalSeasonEpisodeSuffix);
    }

    private static SiftPattern<Fragment> seasonEpisodeGapPattern(SiftPattern<Fragment> dashOrTilde){

        return find(dashOrTilde)
                .then().character('s')
                .then().between(1, 3).digits()
                .then().optional().character('e');
    }
}