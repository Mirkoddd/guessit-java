package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.literal;
import static io.guessit.core.text.patterns.CommonPatterns.compileCaseInsensitive;
import static io.guessit.core.text.patterns.CommonPatterns.find;

public final class EpisodeFormatPatterns {

    private EpisodeFormatPatterns() {
    }

    public static Pattern buildMinisodesPattern() {
        var minisodeLiteral = literal("minisode");
        var optionalTrailingS = optional().character('s');

        var basePattern = find(minisodeLiteral)
                .followedBy(optionalTrailingS);

        return compileCaseInsensitive(basePattern);
    }
}