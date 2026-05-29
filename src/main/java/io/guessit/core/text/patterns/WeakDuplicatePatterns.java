package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.namedCapture;

public final class WeakDuplicatePatterns {
    private WeakDuplicatePatterns() {}

    public static Pattern buildPattern(String seasonGroupName, String episodeGroupName) {
        var seasonCapture = capture(seasonGroupName, between(1, 2).digits());
        var episodeCapture = capture(episodeGroupName, exactly(2).digits());

        var boundaryDigit = exactly(1).digits();

        var pattern = namedCapture(seasonCapture)
                .notPrecededBy(boundaryDigit)
                .then()
                .namedCapture(episodeCapture)
                .notFollowedBy(boundaryDigit);

        return Pattern.compile(pattern.shake());
    }
}