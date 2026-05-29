package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.*;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class EpisodeRangePatterns {

    private EpisodeRangePatterns() {}

    public static Pattern buildRangePattern(String numGroupName) {
        var fillingSeparator = anyOfCharacters(' ', '.', '_');

        var dashBranch = dashBranch(fillingSeparator);
        var toBranch = toBranch(fillingSeparator);

        var separatorAlt = anyOf(dashBranch, toBranch);

        var numCapture = capture(numGroupName, oneOrMore().digits());

        var pattern = find(separatorAlt).followedBy(namedCapture(numCapture));

        return compileCaseInsensitive(pattern);
    }

    private static SiftPattern<Fragment> dashBranch(SiftPattern<Fragment> fillingSeparator) {
        var intervalSeparator = anyOfCharacters('-', '~');
        var zeroOrMoreSeparators = zeroOrMore().of(fillingSeparator);

        return zeroOrMoreSeparators
                .followedBy(intervalSeparator)
                .followedBy(zeroOrMoreSeparators);
    }

    private static SiftPattern<Fragment> toBranch(SiftPattern<Fragment> fillingSeparator) {
        var intervalSeparator = literal("to");
        var oneOrMoreSeparators = oneOrMore().of(fillingSeparator);

        return oneOrMoreSeparators
                .followedBy(intervalSeparator)
                .followedBy(oneOrMoreSeparators);
    }
}