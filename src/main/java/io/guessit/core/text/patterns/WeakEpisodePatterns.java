package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Connector;
import com.mirkoddd.sift.core.dsl.Fragment;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.namedCapture;

public final class WeakEpisodePatterns {
    private WeakEpisodePatterns() {}

    public static Pattern buildPatternSingleDigit(String episodeGroupName){
        return buildPattern(exactly(1).digits(), episodeGroupName);
    }

    public static Pattern buildPatternTwoDigits(String episodeGroupName){
        return buildPattern(exactly(2).digits(), episodeGroupName);
    }

    public static Pattern buildPatternThreeOrFourDigits(String episodeGroupName){
        return buildPattern(between(3, 4).digits(), episodeGroupName);
    }

    private static Pattern buildPattern(Connector<Fragment> digitsPattern, String episodeGroupName) {
        var optionalVersion = optional().of(
                exactly(1).character('v').then().oneOrMore().digits()
        );

        var episodeCapture = capture(episodeGroupName, digitsPattern);

        var boundaryDigit = exactly(1).digits();

        var pattern = namedCapture(episodeCapture)
                .notPrecededBy(boundaryDigit)
                .followedBy(optionalVersion)
                .notFollowedBy(boundaryDigit);

        return Pattern.compile(pattern.shake());
    }
}