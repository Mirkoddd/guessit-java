package io.guessit.core.text.patterns;

import java.util.List;
import java.util.regex.Pattern;
import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.*;
import static io.guessit.core.text.patterns.CommonPatterns.*;

/**
 * Encapsulates Sift regex logic specifically for Release Group extraction.
 */
public final class ReleaseGroupPatterns {

    private ReleaseGroupPatterns() {}

    public static Pattern buildParensBracketsPattern(String groupMain, String groupSub) {
        var mainCapture = capture(groupMain, oneOrMore().anyCharacter());
        var subCapture = capture(groupSub, oneOrMore().anyCharacter());

        var pattern = namedCapture(mainCapture)
                .then().character(')')
                .then().optional().whitespace()
                .then().character('[')
                .then().namedCapture(subCapture)
                .then().character(']');

        return Pattern.compile(pattern.shake());
    }

    private static final List<String> TRAILING_EXTENSIONS = List.of(
            "mkv", "mp4", "avi", "mov", "m4v", "mpeg", "mpg", "ts", "m2ts",
            "wmv", "webm", "flv", "ogg", "ogm", "ogv", "iso", "3gp", "3g2",
            "3gp2", "asf", "divx", "mka", "mk2", "mk3d", "mp4a", "qt", "ra",
            "ram", "rm", "vob", "wav", "wma", "srt", "idx", "sub", "ssa",
            "ass", "nfo", "torrent", "nzb"
    );

    public static Pattern buildKnownTrailingExtPattern() {
        var anyOfExtensions = anyOfStringsInList(TRAILING_EXTENSIONS);

        var pattern = exactly(1).character('.')
                .followedBy(anyOfExtensions)
                .andNothingElse();

        return compileCaseInsensitive(pattern);
    }
}